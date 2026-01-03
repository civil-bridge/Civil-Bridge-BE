package org.example.gyeonggi_partners.domain.proposal.infra.persistence;

import lombok.RequiredArgsConstructor;
import org.example.gyeonggi_partners.domain.proposal.domain.model.Proposal;
import org.example.gyeonggi_partners.domain.proposal.domain.repository.ProposalRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProposalRepositoryImpl implements ProposalRepository {

    private final ProposalJpaRepository proposalJpaRepository;

    @Override
    public Proposal save(Proposal proposal) {
        ProposalEntity entity = ProposalEntity.fromDomain(proposal);
        ProposalEntity savedEntity = proposalJpaRepository.save(entity);
        
        return savedEntity.toDomain();
    }

    @Override
    public Optional<Proposal> findById(Long proposalId) {
        Optional<ProposalEntity> proposalEntity = proposalJpaRepository.findById(proposalId);

        return proposalEntity.map(ProposalEntity::toDomain);
    }

    @Override
    public List<Proposal> findVotingProposalsWithExpiredDeadline(LocalDateTime now) {
        return proposalJpaRepository.findVotingProposalsWithExpiredDeadline(now).stream()
                .map(ProposalEntity::toDomain)
                .toList();
    }

    @Override
    public List<Proposal> findByRoomId(Long roomId) {
        return proposalJpaRepository.findByRoomId(roomId).stream()
                .map(ProposalEntity::toDomain)
                .toList();
    }

    @Override
    public int countByRoomId(Long roomId) {
        return proposalJpaRepository.countByRoomId(roomId);
    }
}
