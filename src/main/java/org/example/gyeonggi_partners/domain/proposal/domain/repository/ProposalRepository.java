package org.example.gyeonggi_partners.domain.proposal.domain.repository;

import org.example.gyeonggi_partners.domain.proposal.domain.model.Proposal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProposalRepository {

    Proposal save(Proposal proposal);

    Optional<Proposal> findById(Long proposalId);

    List<Proposal> findVotingProposalsWithExpiredDeadline(LocalDateTime now);

    List<Proposal> findByRoomId(Long roomId);

    int countByRoomId(Long roomId);
}
