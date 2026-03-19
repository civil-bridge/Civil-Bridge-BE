package org.example.civilbridge.domain.discussionRoom.application;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.civilbridge.common.exception.BusinessException;
import org.example.civilbridge.domain.discussionRoom.api.dto.*;
import org.example.civilbridge.domain.discussionRoom.api.dto.DiscussionRoomListRes;
import org.example.civilbridge.domain.discussionRoom.domain.model.DiscussionRoom;
import org.example.civilbridge.domain.discussionRoom.domain.model.Member;
import org.example.civilbridge.domain.discussionRoom.domain.repository.DiscussionRoomRepository;
import org.example.civilbridge.domain.discussionRoom.domain.repository.MemberRepository;
import org.example.civilbridge.domain.discussionRoom.exception.DiscussionRoomErrorCode;
import org.example.civilbridge.domain.discussionRoom.infra.cache.DiscussionRoomCacheRepository;
import org.example.civilbridge.domain.discussionRoom.infra.cache.dto.DiscussionRoomCacheModel;
import org.example.civilbridge.domain.user.domain.model.User;
import org.example.civilbridge.domain.user.domain.repository.UserRepository;
import org.example.civilbridge.domain.user.exception.UserErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscussionRoomService {

    private final DiscussionRoomRepository discussionRoomRepository;
    private final MemberRepository memberRepository;
    private final DiscussionRoomCacheRepository cacheRepository;
    private final UserRepository userRepository;
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate readOnlyTx;

    @PostConstruct
    private void initTransactionTemplate() {
        readOnlyTx = new TransactionTemplate(transactionManager);
        readOnlyTx.setReadOnly(true);
    }

    private record JoinedRoomsDbResult(
        List<DiscussionRoom> rooms,
        List<Long> roomIds,
        Map<Long, Integer> memberCountMap,
        long totalElements
    ) {}

    private record TotalRoomsDbResult(
        List<DiscussionRoom> rooms,
        List<Long> roomIds,
        Map<Long, Integer> memberCountMap,
        long totalElements
    ) {}

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
            request.getCity(),
            request.getDistrict(),
            request.getAccessLevel()
        );

        // 2. DB 저장
        DiscussionRoom savedRoom = discussionRoomRepository.save(room);
        log.debug("DB 저장 완료 - roomId: {}", savedRoom.getId());
        
        // 3. 생성자를 멤버로 추가 (방장은 자동으로 참여)
        Member creatorMember = Member.join(userId, savedRoom.getId());
        memberRepository.save(creatorMember);
        log.debug("생성자 멤버 추가 완료 - userId: {}, roomId: {}", userId, savedRoom.getId());
        
        // 4. Redis 캐싱 (Write-Through 전략)
        DiscussionRoomCacheModel model = DiscussionRoomCacheModel.fromDomainModel(savedRoom, 1);
        cacheRepository.saveNewRoomToRedis(model, userId, System.currentTimeMillis());

        log.debug("Redis 캐싱 완료 - roomId: {}", savedRoom.getId());

        // 5. 생성자 정보 조회 후 멤버 목록 구성 (생성자 = 방장)
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        List<MemberInfo> members = List.of(
                MemberInfo.builder()
                        .userId(creator.getId())
                        .nickname(creator.getNickname())
                        .role("LEADER")
                        .profileImageUrl(null)
                        .build()
        );

        log.info("논의방 생성 성공 - roomId: {}", savedRoom.getId());

        return JoinRoomRes.of(model, members);
    }

    @Transactional
    public JoinRoomRes joinRoom(Long userId, Long roomId) {
        log.info("논의방 입장 요청 - userId: {}, roomId: {}", userId, roomId);

        // 1. 이미 참여 중인 경우 → 방 정보만 반환 (멱등성 보장)
        if (memberRepository.existsByUserIdAndRoomId(userId, roomId)) {
            log.info("이미 참여 중인 논의방 재입장 - userId: {}, roomId: {}", userId, roomId);
            return buildRoomInfo(roomId);
        }

        // 2. 방 존재 확인 (캐시 미스 시 DB 조회 후 캐싱)
        cacheRepository.retrieveCachingRoom(roomId)
                .orElseThrow(() -> new BusinessException(DiscussionRoomErrorCode.ROOM_NOT_FOUND));

        // 3. DB에 멤버 추가
        Member member = Member.join(userId, roomId);
        memberRepository.save(member);
        log.debug("멤버 추가 완료 - userId: {}, roomId: {}", userId, roomId);

        // 4. Redis 업데이트 (currentUsers HINCRBY +1)
        cacheRepository.addUserToRoom(userId, roomId, System.currentTimeMillis());
        log.debug("Redis 업데이트 완료 - roomId: {}", roomId);

        log.info("논의방 입장 성공 - userId: {}, roomId: {}", userId, roomId);
        return buildRoomInfo(roomId);
    }

    @Transactional(readOnly = true)
    public JoinRoomRes getRoomDetail(Long roomId) {
        log.info("논의방 상세 조회 - roomId: {}", roomId);
        return buildRoomInfo(roomId);
    }

    private JoinRoomRes buildRoomInfo(Long roomId) {
        DiscussionRoomCacheModel room = cacheRepository.retrieveCachingRoom(roomId)
                .orElseThrow(() -> new BusinessException(DiscussionRoomErrorCode.ROOM_NOT_FOUND));

        // 멤버 ID 목록: Redis 우선 조회
        // 단, 캐시 리스트 크기가 실제 인원수(currentUsers)와 다르면 stale 데이터로 간주
        // → DB에서 전체 재조회 후 Redis 재캐싱
        List<Long> memberIds = cacheRepository.retrieveRoomMembers(roomId);
        if (memberIds.size() != room.getCurrentUsers()) {
            log.debug("Redis 멤버 목록 불일치 (cached={}, expected={}) - DB 재조회 및 재캐싱 - roomId: {}",
                    memberIds.size(), room.getCurrentUsers(), roomId);
            memberIds = memberRepository.findUserIdsByRoomId(roomId);  // createdAt ASC
            cacheRepository.cacheRoomMembers(roomId, memberIds);
        }

        // 방장: 참여 시각 기준 첫 번째 멤버 (memberIds는 createdAt ASC 순서 보장)
        Long leaderUserId = memberIds.isEmpty() ? null : memberIds.get(0);

        // 유저 정보 일괄 조회 후 MemberInfo 변환 (N+1 방지: IN 쿼리 1회)
        List<User> users = userRepository.findAllByIdIn(memberIds);
        List<MemberInfo> members = buildMemberInfoList(users, leaderUserId);

        return JoinRoomRes.of(room, members);
    }

    private List<MemberInfo> buildMemberInfoList(List<User> users, Long leaderUserId) {
        return users.stream()
                .map(user -> MemberInfo.builder()
                        .userId(user.getId())
                        .nickname(user.getNickname())
                        .role(user.getId().equals(leaderUserId) ? "LEADER" : "PARTICIPANT")
                        .profileImageUrl(null)
                        .build())
                .collect(Collectors.toList());
    }






    /**
     * 전체 논의방 목록 조회 (최신순, 페이징)
     * Cache-Aside 전략: 목록은 DB에서 조회, 개별 방 정보는 캐시 활용
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 논의방 목록 및 페이징 정보
     */
    public DiscussionRoomListRes retrieveRoomsByPage(int page, int size) {
        log.info("전체 논의방 목록 조회 - page: {}, size: {}", page, size);

        // 1. DB 쿼리 — 트랜잭션 안에서 완료 후 커넥션 반납
        TotalRoomsDbResult dbResult = readOnlyTx.execute(status -> {
            Pageable pageable = PageRequest.of(page - 1, size);
            Page<DiscussionRoom> roomPage = discussionRoomRepository.findAllByOrderByCreatedAtDesc(pageable);

            if (roomPage.isEmpty()) {
                log.debug("조회된 논의방 없음");
                return null;
            }

            List<DiscussionRoom> rooms = roomPage.getContent();
            List<Long> roomIds = rooms.stream()
                    .map(DiscussionRoom::getId)
                    .collect(Collectors.toList());
            Map<Long, Integer> memberCountMap = memberRepository.countByRoomIds(roomIds);

            return new TotalRoomsDbResult(rooms, roomIds, memberCountMap, roomPage.getTotalElements());
        });

        if (dbResult == null) {
            return DiscussionRoomListRes.of(List.of(), page, size, 0);
        }

        // 2. Redis Pipeline 조회 — 트랜잭션 밖, 한 번의 왕복
        Map<Long, Optional<DiscussionRoomCacheModel>> cacheResults =
                cacheRepository.getCachedRoomsAll(dbResult.roomIds());

        // 3. 응답 조립 (캐시 미스 시 메모리 데이터로 조립 후 캐싱)
        List<DiscussionRoomInfo> roomSummaries = dbResult.rooms().stream()
                .map(room -> {
                    DiscussionRoomCacheModel cached = cacheResults.get(room.getId())
                            .orElseGet(() -> {
                                int currentUsers = dbResult.memberCountMap().getOrDefault(room.getId(), 0);
                                DiscussionRoomCacheModel model = DiscussionRoomCacheModel.fromDomainModel(room, currentUsers);
                                cacheRepository.cacheRoomInfo(model);
                                return model;
                            });
                    return DiscussionRoomInfo.from(cached);
                })
                .collect(Collectors.toList());

        log.info("전체 논의방 목록 조회 성공 - 조회된 방: {}개, 전체: {}개",
                roomSummaries.size(), dbResult.totalElements());

        return DiscussionRoomListRes.of(roomSummaries, page, size, dbResult.totalElements());
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
    public DiscussionRoomListRes retrieveJoinedRooms(Long userId, int page, int size) {
        log.info("내가 참여한 논의방 목록 조회 - userId: {}, page: {}, size: {}", userId, page, size);

        // 1. DB 쿼리 — 트랜잭션 안에서 완료 후 커넥션 반납
        JoinedRoomsDbResult dbResult = readOnlyTx.execute(status -> {
            Pageable pageable = PageRequest.of(page - 1, size);
            Page<DiscussionRoom> roomPage = memberRepository.findRoomsByUserId(userId, pageable);

            if (roomPage.isEmpty()) {
                log.debug("참여한 논의방 없음 - userId: {}", userId);
                return null;
            }

            List<DiscussionRoom> rooms = roomPage.getContent();
            List<Long> roomIds = rooms.stream()
                    .map(DiscussionRoom::getId)
                    .collect(Collectors.toList());
            Map<Long, Integer> memberCountMap = memberRepository.countByRoomIds(roomIds);

            return new JoinedRoomsDbResult(rooms, roomIds, memberCountMap, roomPage.getTotalElements());
        });

        if (dbResult == null) {
            return DiscussionRoomListRes.of(List.of(), page, size, 0);
        }

        // 2. Redis Pipeline 조회 — 트랜잭션 밖, 한 번의 왕복
        Map<Long, Optional<DiscussionRoomCacheModel>> cacheResults =
                cacheRepository.getCachedRoomsAll(dbResult.roomIds());

        // 3. 응답 조립 (캐시 미스 시 메모리 데이터로 조립 후 캐싱)
        List<DiscussionRoomInfo> roomSummaries = dbResult.rooms().stream()
                .map(room -> {
                    DiscussionRoomCacheModel cached = cacheResults.get(room.getId())
                            .orElseGet(() -> {
                                int currentUsers = dbResult.memberCountMap().getOrDefault(room.getId(), 0);
                                DiscussionRoomCacheModel model = DiscussionRoomCacheModel.fromDomainModel(room, currentUsers);
                                cacheRepository.cacheRoomInfo(model);
                                return model;
                            });
                    return DiscussionRoomInfo.from(cached);
                })
                .collect(Collectors.toList());

        log.info("내가 참여한 논의방 목록 조회 성공 - userId: {}, 조회된 방: {}개, 전체: {}개",
                userId, roomSummaries.size(), dbResult.totalElements());

        return DiscussionRoomListRes.of(roomSummaries, page, size, dbResult.totalElements());
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

}
