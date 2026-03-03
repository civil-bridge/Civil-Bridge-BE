package org.example.civilbridge.domain.proposal.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProposalJpaRepository extends JpaRepository<ProposalEntity, Long> {

    @Query("SELECT p FROM ProposalEntity p WHERE p.status = 'VOTING' AND p.deadline < :now")
    List<ProposalEntity> findVotingProposalsWithExpiredDeadline(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM ProposalEntity p WHERE p.roomId = :roomId ORDER BY p.createdAt DESC")
    List<ProposalEntity> findByRoomId(@Param("roomId") Long roomId);

    int countByRoomId(Long roomId);

    // @Version 체크를 우회하는 직접 업데이트 (Redis 락이 단일 작성자를 이미 보장)
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE proposals SET title = :title, contents = :contentsJson, updated_at = :updatedAt WHERE proposal_id = :proposalId",
            nativeQuery = true)
    void updateContent(@Param("proposalId") Long proposalId,
                       @Param("title") String title,
                       @Param("contentsJson") String contentsJson,
                       @Param("updatedAt") LocalDateTime updatedAt);

    // 최종 제출: content 저장 + 투표 상태 전환을 단일 쿼리로 원자적 처리 (@Version 우회)
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE proposals SET title = :title, contents = :contentsJson, status = 'VOTING', consent_deadline = :deadline, updated_at = :updatedAt WHERE proposal_id = :proposalId",
            nativeQuery = true)
    void submitAndStartVoting(@Param("proposalId") Long proposalId,
                              @Param("title") String title,
                              @Param("contentsJson") String contentsJson,
                              @Param("deadline") LocalDateTime deadline,
                              @Param("updatedAt") LocalDateTime updatedAt);

    // 동의 추가: consents JSON 배열에 새 동의자를 append (@Version 우회, 스케줄러와의 충돌 방지)
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE proposals SET consents = JSON_ARRAY_APPEND(COALESCE(consents, JSON_ARRAY()), '$', CAST(:consenterJson AS JSON)), updated_at = :updatedAt WHERE proposal_id = :proposalId",
            nativeQuery = true)
    void addConsent(@Param("proposalId") Long proposalId,
                    @Param("consenterJson") String consenterJson,
                    @Param("updatedAt") LocalDateTime updatedAt);
}
