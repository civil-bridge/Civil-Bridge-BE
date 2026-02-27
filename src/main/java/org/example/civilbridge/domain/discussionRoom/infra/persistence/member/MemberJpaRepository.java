package org.example.civilbridge.domain.discussionRoom.infra.persistence.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Member JPA Repository
 * Spring Data JPA 인터페이스
 */
public interface MemberJpaRepository extends JpaRepository<MemberEntity, Long> {

    /**
     * 사용자와 논의방으로 중복 참여 확인
     */
    boolean existsByUserIdAndRoomId(Long userId, Long roomId);

    /**
     * 멤버 삭제 (논의방 나가기)
     */
    void deleteByUserIdAndRoomId(Long userId, Long roomId);

    /**
     * 논의방의 남은 멤버 수 조회
     */
    int countByRoomId(Long roomId);

    /**
     * 사용자가 참여한 논의방 ID 목록 조회 (최신 참여순)
     */
    @Query("SELECT m.roomId FROM MemberEntity m WHERE m.userId = :userId ORDER BY m.createdAt DESC")
    Page<Long> findRoomIdsByUserIdOrderByJoinedAtDesc(Long userId, Pageable pageable);

    /**
     * 여러 논의방의 멤버 수 일괄 조회 (N+1 쿼리 방지)
     * @param roomIds 논의방 ID 목록
     * @return roomId별 멤버 수 (Projection)
     */
    @Query("SELECT m.roomId as roomId, COUNT(m) as count FROM MemberEntity m WHERE m.roomId IN :roomIds GROUP BY m.roomId")
    List<RoomMemberCount> countByRoomIds(@Param("roomIds") List<Long> roomIds);

    /**
     * Projection 인터페이스: roomId별 멤버 수
     */
    interface RoomMemberCount {
        Long getRoomId();
        Long getCount();
    }

    /**
     * 논의방의 모든 멤버 userId 목록 조회 (참여 시각 오름차순)
     */
    @Query("SELECT m.userId FROM MemberEntity m WHERE m.roomId = :roomId ORDER BY m.createdAt ASC")
    List<Long> findUserIdsByRoomIdOrderByCreatedAt(@Param("roomId") Long roomId);
}
