package org.example.gyeonggi_partners.domain.discussionRoom.infra.persistence.discussionRoom;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * DiscussionRoom JPA Repository
 * Spring Data JPA 인터페이스
 */
public interface DiscussionRoomJpaRepository extends JpaRepository<DiscussionRoomEntity, Long> {

    /**
     * Soft Delete: deletedAt을 현재 시각으로 업데이트
     * 실제 데이터는 삭제하지 않고 삭제 표시만 함 (범죄 수사 대응)
     */
    @Modifying
    @Query("UPDATE DiscussionRoomEntity d SET d.deletedAt = CURRENT_TIMESTAMP WHERE d.id = :id")
    void softDelete(@Param("id") Long id);

    Optional<DiscussionRoomEntity> findById(Long discussionRoomId);

    /**
     * 전체 논의방 목록 조회 (삭제되지 않은 것만, 최신순)
     */
    @Query("SELECT d FROM DiscussionRoomEntity d WHERE d.deletedAt IS NULL ORDER BY d.createdAt DESC")
    Page<DiscussionRoomEntity> findAllByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);

    /**
     * ID 목록으로 논의방 일괄 조회 (N+1 쿼리 방지)
     * 삭제되지 않은 것만 조회
     */
    @Query("SELECT d FROM DiscussionRoomEntity d WHERE d.id IN :ids AND d.deletedAt IS NULL")
    List<DiscussionRoomEntity> findAllByIdIn(@Param("ids") List<Long> ids);

    /**
     * 논의방 ID로 조회 (비관적 락)
     * SELECT FOR UPDATE를 사용하여 동시성 제어
     * 다른 트랜잭션의 읽기/쓰기를 차단하고 배타적 잠금 획득
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DiscussionRoomEntity d WHERE d.id = :roomId AND d.deletedAt IS NULL")
    Optional<DiscussionRoomEntity> findByIdWithLock(@Param("roomId") Long roomId);
}
