package org.example.civilbridge.domain.discussionRoom.domain.repository;

import org.example.civilbridge.domain.discussionRoom.domain.model.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Member 도메인 Repository 인터페이스
 * 도메인 계층에서 정의하고, 인프라 계층에서 구현
 */
public interface MemberRepository {

    /**
     * 멤버 저장 (논의방 참여)
     * @param member 저장할 멤버
     * @return 저장된 멤버 (ID 포함)
     */
    Member save(Member member);

    /**
     * 중복 참여 확인
     * @param userId 사용자 ID
     * @param roomId 논의방 ID
     * @return 이미 참여 중이면 true
     */
    boolean existsByUserIdAndRoomId(Long userId, Long roomId);

    /**
     * 멤버 삭제 (논의방 나가기)
     * @param userId 사용자 ID
     * @param roomId 논의방 ID
     */
    void deleteByUserIdAndRoomId(Long userId, Long roomId);

    /**
     * 논의방의 남은 멤버 수 조회
     * @param roomId 논의방 ID
     * @return 남은 멤버 수
     */
    int countByRoomId(Long roomId);

    /**
     * 사용자가 참여한 논의방 ID 목록 조회 (페이징, 최신 참여순)
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 논의방 ID 목록
     */
    Page<Long> findRoomIdsByUserId(Long userId, Pageable pageable);

    /**
     * 여러 논의방의 멤버 수 일괄 조회 (N+1 쿼리 방지)
     * @param roomIds 논의방 ID 목록
     * @return roomId를 키로, 멤버 수를 값으로 하는 Map
     */
    Map<Long, Integer> countByRoomIds(List<Long> roomIds);

    /**
     * 논의방의 모든 멤버 userId 목록 조회 (참여 시각 오름차순)
     * @param roomId 논의방 ID
     * @return 참여 시각 순으로 정렬된 사용자 ID 목록
     */
    List<Long> findUserIdsByRoomId(Long roomId);

    /**
     * 논의방 방장(가장 먼저 참여한 멤버) userId 조회
     * @param roomId 논의방 ID
     * @return 방장의 사용자 ID (방이 비어있을 경우 empty)
     */
    Optional<Long> findLeaderUserIdByRoomId(Long roomId);
}
