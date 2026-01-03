package org.example.gyeonggi_partners.domain.proposal.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gyeonggi_partners.domain.proposal.domain.model.Proposal;
import org.example.gyeonggi_partners.domain.proposal.domain.repository.ProposalRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProposalVotingScheduler {

    private final ProposalRepository proposalRepository;

    /**
     * 매 시간마다 투표 기간 만료된 제안서 확인
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void checkExpiredVoting() {
        log.info("투표기간 만료된 제안서 체크 시작");

        List<Proposal> expiredProposals = proposalRepository.findVotingProposalsWithExpiredDeadline(LocalDateTime.now());

        for(Proposal proposal : expiredProposals) {
            try {
                proposal.endVoting();
                proposalRepository.save(proposal);
                log.info("제안서 {} 투표 자동 죵로 완료 (동의자 수 : {})", proposal.getId(), proposal.getConsents().size());
            } catch (Exception e) {
                log.info("제안서 {} 투표 종료 실패: {}", proposal.getId(), e.getMessage());
            }
        }

        log.info("투표기간 만료된 제안서 체크 완료");
    }
}
