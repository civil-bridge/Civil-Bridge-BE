package org.example.gyeonggi_partners.domain.discussionRoom.application;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gyeonggi_partners.common.exception.BusinessException;
import org.example.gyeonggi_partners.domain.discussionRoom.api.dto.*;
import org.example.gyeonggi_partners.domain.discussionRoom.api.dto.DiscussionRoomListRes;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.model.DiscussionRoom;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.model.Member;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.repository.DiscussionRoomRepository;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.repository.MemberRepository;
import org.example.gyeonggi_partners.domain.discussionRoom.exception.DiscussionRoomErrorCode;
import org.example.gyeonggi_partners.domain.discussionRoom.infra.cache.DiscussionRoomCacheRepository;
import org.example.gyeonggi_partners.domain.discussionRoom.infra.cache.dto.DiscussionRoomCacheModel;
import org.example.gyeonggi_partners.domain.discussionRoom.infra.cache.dto.DiscussionRoomsPage;
import org.example.gyeonggi_partners.domain.user.domain.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscussionRoomService {

    private final DiscussionRoomRepository discussionRoomRepository;
    private final MemberRepository memberRepository;
    private final DiscussionRoomCacheRepository cacheRepository;
    private final UserRepository userRepository;

    /**
     * 논의방 생성
     * Write-Through 전략: DB 저장 후 Redis 캐싱
     * 
     * @param request 논의방 생성 요청
     * @param userId 생성자 ID (현재 로그인한 사용자)
     * @return 생성된 논의방 정보 (입장 완료 상태)
     */
    @Transactional
    public JoinRoomRes createRoom(CreateDiscussionRoomReq request, Long userId) {
        log.info("논의방 생성 요청 - userId: {}, title: {}", userId, request.getTitle());
        
        // 1. Domain 생성 (비즈니스 로직 & 유효성 검증)
        DiscussionRoom room = DiscussionRoom.create(
            request.getTitle(),
            request.getDescription(),
            request.getRegion(),
            request.getAccessLevel()
        );

        // 2. DB 저장
        DiscussionRoom savedRoom = discussionRoomRepository.save(room);
        log.debug("DB 저장 완료 - roomId: {}", savedRoom.getId());
        
        // 3. 생성자를 멤버로 추가 (방장은 자동으로 참여)
        Member creator = Member.join(userId, savedRoom.getId());
        memberRepository.save(creator);
        log.debug("생성자 멤버 추가 완료 - userId: {}, roomId: {}", userId, savedRoom.getId());
        
        // 4. Redis 캐싱 (Write-Through 전략)
        DiscussionRoomCacheModel model = DiscussionRoomCacheModel.fromDomainModel(savedRoom, 1);
        cacheRepository.saveNewRoomToRedis(model, userId, System.currentTimeMillis());

        log.debug("Redis 캐싱 완료 - roomId: {}", savedRoom.getId());
        
        // 5. 멤버 목록 조회 (현재는 생성자만 존재)
        List<Long> memberIds = List.of(userId);

        // 6. 멤버 ID를 닉네임으로 변환 (추가된 부분)
        List<String> memberNicknames = getNicknamesFromIds(memberIds);

        // 6. 멤버 ID를 닉네임으로 변환 (User 도메인과의 통합 필요)

        // 6. JoinRoomRes 반환 (생성 = 입장 완료)
        log.info("논의방 생성 성공 - roomId: {}", savedRoom.getId());

        return JoinRoomRes.of(model, memberNicknames);
    }

    @Transactional
    public JoinRoomRes joinRoom(Long userId, Long roomId) {
        log.info("논의방 입장 요청 - userId: {}, roomId: {}", userId, roomId);

        // 1. 중복 참여 확인
        if (memberRepository.existsByUserIdAndRoomId(userId, roomId)) {
            throw new BusinessException(DiscussionRoomErrorCode.ALREADY_JOINED_ROOM);
        }

        // 2. 방 정보 조회 (캐시 미스 시 DB 조회 후 캐싱)
        DiscussionRoomCacheModel cachedRoom = cacheRepository.retrieveCachingRoom(roomId)
                .orElseThrow(() -> new BusinessException(DiscussionRoomErrorCode.ROOM_NOT_FOUND));

        // 3. DB에 멤버 추가
        Member member = Member.join(userId, roomId);
        memberRepository.save(member);
        log.debug("멤버 추가 완료 - userId: {}, roomId: {}", userId, roomId);

        // 4. Redis 업데이트
        cacheRepository.addUserToRoom(userId, roomId, System.currentTimeMillis());
        log.debug("Redis 업데이트 완료 - roomId: {}", roomId);

        // 5. 멤버 목록 조회
        List<Long> memberIds = cacheRepository.retrieveRoomMembers(roomId);

        // 6. 멤버 ID를 닉네임으로 변환 (추가된 부분)
        List<String> memberNicknames = getNicknamesFromIds(memberIds);

        log.info("논의방 입장 성공 - userId: {}, roomId: {}", userId, roomId);
        return JoinRoomRes.of(cachedRoom, memberNicknames); // 수정된 부분
    }






    /**
     * 전체 논의방 목록 조회 (최신순, 페이징)
     * Cache-Aside 전략: 목록은 DB에서 조회, 개별 방 정보는 캐시 활용
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 논의방 목록 및 페이징 정보
     */
    @Transactional(readOnly = true)
    public DiscussionRoomListRes retrieveTotalRooms(int page, int size) {
        log.info("전체 논의방 목록 조회 - page: {}, size: {}", page, size);

        // 1. DB에서 논의방 목록 조회 (페이징) - DB가 source of truth
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<DiscussionRoom> roomPage = discussionRoomRepository.findAllByOrderByCreatedAtDesc(pageable);

        if (roomPage.isEmpty()) {
            log.debug("조회된 논의방 없음");
            return DiscussionRoomListRes.of(List.of(), page, size, 0);
        }

        // 2. 각 방 상세 정보 조회 (N+1 쿼리 방지: 멤버 수 일괄 조회)
        List<DiscussionRoom> rooms = roomPage.getContent();
        List<Long> roomIds = rooms.stream()
                .map(DiscussionRoom::getId)
                .collect(Collectors.toList());

        // 멤버 수 일괄 조회 (N+1 방지)
        Map<Long, Integer> memberCountMap = memberRepository.countByRoomIds(roomIds);

        List<DiscussionRoomInfo> roomSummaries = rooms.stream()
                .map(room -> {
                    // retrieveCachingRoom: 캐시 미스 시 DB 조회 후 캐싱
                    DiscussionRoomCacheModel cached = cacheRepository.retrieveCachingRoom(room.getId())
                            .orElseGet(() -> {
                                int currentUsers = memberCountMap.getOrDefault(room.getId(), 0);
                                return DiscussionRoomCacheModel.fromDomainModel(room, currentUsers);
                            });
                    return DiscussionRoomInfo.from(cached);
                })
                .collect(Collectors.toList());

        log.info("전체 논의방 목록 조회 성공 - 조회된 방: {}개, 전체: {}개",
                roomSummaries.size(), roomPage.getTotalElements());

        return DiscussionRoomListRes.of(
                roomSummaries,
                page,
                size,
                roomPage.getTotalElements()
        );
    }

    /**
     * 사용자가 참여한 논의방 목록 조회 (최신 참여순, 페이징)
     * Cache-Aside 전략: 목록은 DB에서 조회, 개별 방 정보는 캐시 활용
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 논의방 목록 및 페이징 정보
     */
    @Transactional(readOnly = true)
    public DiscussionRoomListRes retrieveJoinedRooms(Long userId, int page, int size) {
        log.info("내가 참여한 논의방 목록 조회 - userId: {}, page: {}, size: {}", userId, page, size);

        // 1. DB에서 사용자가 참여한 방 ID 목록 조회 (페이징) - DB가 source of truth
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Long> roomIdPage = memberRepository.findRoomIdsByUserId(userId, pageable);

        if (roomIdPage.isEmpty()) {
            log.debug("참여한 논의방 없음 - userId: {}", userId);
            return DiscussionRoomListRes.of(List.of(), page, size, 0);
        }

        // 2. 각 방 상세 정보 조회 (N+1 쿼리 방지: 일괄 조회)
        List<Long> roomIds = roomIdPage.getContent();
        List<DiscussionRoom> rooms = discussionRoomRepository.findAllByIdIn(roomIds);

        // 멤버 수 일괄 조회 (N+1 방지)
        Map<Long, Integer> memberCountMap = memberRepository.countByRoomIds(roomIds);

        List<DiscussionRoomInfo> roomSummaries = rooms.stream()
                .map(room -> {
                    // retrieveCachingRoom: 캐시 미스 시 DB 조회 후 캐싱
                    DiscussionRoomCacheModel cached = cacheRepository.retrieveCachingRoom(room.getId())
                            .orElseGet(() -> {
                                int currentUsers = memberCountMap.getOrDefault(room.getId(), 0);
                                return DiscussionRoomCacheModel.fromDomainModel(room, currentUsers);
                            });
                    return DiscussionRoomInfo.from(cached);
                })
                .collect(Collectors.toList());

        log.info("내가 참여한 논의방 목록 조회 성공 - userId: {}, 조회된 방: {}개, 전체: {}개",
                userId, roomSummaries.size(), roomIdPage.getTotalElements());

        return DiscussionRoomListRes.of(
                roomSummaries,
                page,
                size,
                roomIdPage.getTotalElements()
        );
    }

    @Transactional
    public void leaveRoom(Long userId, Long roomId) {
        log.info("논의방 나가기 요청 - userId: {}, roomId: {}", userId, roomId);

        // 1. 비관적 락으로 방 조회 (다른 트랜잭션 대기)
        DiscussionRoom room = discussionRoomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new BusinessException(DiscussionRoomErrorCode.ROOM_NOT_FOUND));
        log.debug("방 잠금 획득 - roomId: {}", roomId);

        // 2. 사용자가 실제로 멤버인지 확인
        if (!memberRepository.existsByUserIdAndRoomId(userId, roomId)) {
            throw new BusinessException(DiscussionRoomErrorCode.NOT_A_ROOM_MEMBER);
        }

        // 3. 삭제 전 남은 인원 확인 (현재 사용자 포함)
        int remainingUsers = memberRepository.countByRoomId(roomId);
        log.debug("삭제 전 인원 - roomId: {}, count: {}", roomId, remainingUsers);

        // 4. Redis 퇴장 처리
        cacheRepository.removeUserFromRoom(userId, roomId);

        // 5. DB 멤버 삭제
        memberRepository.deleteByUserIdAndRoomId(userId, roomId);
        log.debug("멤버 삭제 완료 - userId: {}, roomId: {}", userId, roomId);

        // 6. 마지막 사람이 나가면 방 삭제 (삭제 전 1명이었던 경우)
        if (remainingUsers == 1) {
            log.info("마지막 멤버 퇴장 - 방 삭제 처리 - roomId: {}, lastUserId: {}", roomId, userId);
            discussionRoomRepository.softDelete(roomId);
            cacheRepository.evictRoomCache(roomId, userId);
        }

        log.info("논의방 나가기 성공 - userId: {}, roomId: {}", userId, roomId);
    }

    /**
     * 사용자 ID 목록을 닉네임 목록으로 변환합니다. (로직 변경)
     * UserRepository를 사용하여 실제 닉네임을 조회합니다.
     *
     * @param memberIds 사용자 ID 목록
     * @return 닉네임 목록
     */
    private List<String> getNicknamesFromIds(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return List.of();
        }
        // UserRepository를 사용하여 ID 목록 기반으로 닉네임 목록을 조회합니다.
        return userRepository.findNicknamesByIds(memberIds);
    }
}
