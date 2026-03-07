package org.example.civilbridge.domain.proposal.domain.repository;

import org.example.civilbridge.domain.proposal.domain.model.Consenter;
import org.example.civilbridge.domain.proposal.domain.model.ContentFormat;
import org.example.civilbridge.domain.proposal.domain.model.Proposal;
import org.example.civilbridge.domain.proposal.domain.model.SubmitStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProposalRepository {

    Proposal save(Proposal proposal);

    Optional<Proposal> findById(Long proposalId);

    List<Proposal> findVotingProposalsWithExpiredDeadline(LocalDateTime now);

    List<Proposal> findByRoomId(Long roomId);

    int countByRoomId(Long roomId);

    void updateContent(Long proposalId, String title, ContentFormat contents);

    void submitAndStartVoting(Long proposalId, String title, ContentFormat contents, LocalDateTime deadline, int requiredConsents);

    void addConsent(Long proposalId, Consenter consenter);

    void updateVotingResult(Long proposalId, SubmitStatus status);
}
