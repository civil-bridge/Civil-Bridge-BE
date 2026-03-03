package org.example.civilbridge.domain.proposal.infra.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.civilbridge.domain.proposal.domain.model.Consenter;
import org.example.civilbridge.domain.proposal.domain.model.ContentFormat;
import org.example.civilbridge.domain.proposal.domain.model.Proposal;
import org.example.civilbridge.domain.proposal.domain.repository.ProposalRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProposalRepositoryImpl implements ProposalRepository {

    private final ProposalJpaRepository proposalJpaRepository;
    private final ObjectMapper objectMapper;

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

    @Override
    public void updateContent(Long proposalId, String title, ContentFormat contents) {
        String contentsJson = null;
        if (contents != null) {
            try {
                contentsJson = objectMapper.writeValueAsString(contents);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("ContentFormat 직렬화에 실패했습니다.", e);
            }
        }
        proposalJpaRepository.updateContent(proposalId, title, contentsJson, LocalDateTime.now());
    }

    @Override
    public void submitAndStartVoting(Long proposalId, String title, ContentFormat contents, LocalDateTime deadline, int requiredConsents) {
        String contentsJson = null;
        if (contents != null) {
            try {
                contentsJson = objectMapper.writeValueAsString(contents);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("ContentFormat 직렬화에 실패했습니다.", e);
            }
        }
        proposalJpaRepository.submitAndStartVoting(proposalId, title, contentsJson, deadline, requiredConsents, LocalDateTime.now());
    }

    @Override
    public void addConsent(Long proposalId, Consenter consenter) {
        try {
            String consenterJson = objectMapper.writeValueAsString(consenter);
            proposalJpaRepository.addConsent(proposalId, consenterJson, LocalDateTime.now());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Consenter 직렬화에 실패했습니다.", e);
        }
    }
}
