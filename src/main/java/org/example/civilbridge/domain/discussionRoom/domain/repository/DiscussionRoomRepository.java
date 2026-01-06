package org.example.civilbridge.domain.discussionRoom.domain.repository;

import org.example.civilbridge.domain.discussionRoom.domain.model.DiscussionRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * DiscussionRoom 도메인 Repository 인터페이스
 * 도메인 계층에서 정의하고, 인프라 계층에서 구현
 */
public interface DiscussionRoomRepository {

    /**
     * 논의방 저장 (생성)
     * @param discussionRoom 저장할 논의방
     * @return 저장된 논의방 (ID 포함)
     */
    DiscussionRoom save(DiscussionRoom discussionRoom);

    /**
     * 논의방 ID로 조회
     * @param id 논의방 ID
     * @return 조회된 논의방 (응답 DTO 생성용)
     */
    Optional<DiscussionRoom> findById(Long id);

    /**
     * 논의방 Soft Delete (deletedAt 업데이트)
     * 범죄 수사 등을 위해 실제 데이터는 보존
     * @param roomId 논의방 ID
     */
    void softDelete(Long roomId);

    /**
     * 전체 논의방 목록 조회 (페이징, 최신순)
     * @param pageable 페이징 정보
     * @return 페이징된 논의방 목록
     */
    Page<DiscussionRoom> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * ID 목록으로 논의방 일괄 조회 (N+1 쿼리 방지)
     * @param ids 논의방 ID 목록
     * @return 논의방 목록
     */
    List<DiscussionRoom> findAllByIdIn(List<Long> ids);

    /**
     * 논의방 ID로 조회 (비관적 락)
     * 동시성 제어가 필요한 경우 사용 (SELECT FOR UPDATE)
     * @param id 논의방 ID
     * @return 조회된 논의방 (잠금 획득)
     */
    Optional<DiscussionRoom> findByIdWithLock(Long id);
}
