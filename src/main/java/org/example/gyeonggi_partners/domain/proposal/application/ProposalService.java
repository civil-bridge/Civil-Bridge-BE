package org.example.gyeonggi_partners.domain.proposal.application;

import lombok.RequiredArgsConstructor;
import org.example.gyeonggi_partners.common.exception.BusinessException;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.repository.MemberRepository;
import org.example.gyeonggi_partners.domain.proposal.api.dto.*;
import org.example.gyeonggi_partners.domain.proposal.domain.model.Consenter;
import org.example.gyeonggi_partners.domain.proposal.domain.model.ContentFormat;
import org.example.gyeonggi_partners.domain.proposal.domain.model.Proposal;
import org.example.gyeonggi_partners.domain.proposal.domain.model.SubmitStatus;
import org.example.gyeonggi_partners.domain.proposal.domain.repository.ProposalRepository;
import org.example.gyeonggi_partners.domain.proposal.exception.ProposalErrorCode;
import org.example.gyeonggi_partners.domain.user.domain.model.User;
import org.example.gyeonggi_partners.domain.user.domain.repository.UserRepository;
import org.example.gyeonggi_partners.domain.user.exception.UserErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final UserRepository userRepository;
    private final ProposalLockService lockService;
    private final MemberRepository memberRepository;

    /**
     * 제안서 작성
     */
    public ProposalResponse createProposal(CreateProposalRequest request, Long userId) {

        validateRoomMember(request.getRoomId(), userId);


        int proposalCount = proposalRepository.countByRoomId(request.getRoomId());
        if (proposalCount >= 5) {
            throw new BusinessException(ProposalErrorCode.PROPOSAL_LIMIT_EXCEEDED);
        }

        ContentFormat contents = ContentFormat.of(
                request.getParagraph(),
                request.getImage(),
                request.getSolution(),
                request.getExpectedEffect()
        );

        Proposal proposal = Proposal.create(request.getRoomId(), request.getTitle(), contents);

        Proposal saved = proposalRepository.save(proposal);

        return ProposalResponse.from(saved);
    }

    /**
     * 제안서 조회
     */
    @Transactional(readOnly = true)
    public ProposalResponse getProposal(Long proposalId, Long userId) {


        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND));

        validateRoomMember(proposal.getRoomId(), userId);

        return ProposalResponse.from(proposal);
    }


    /**
     * 제안서 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ProposalResponse> getProposalsByRoom(Long roomId, Long userId) {

        validateRoomMember(roomId, userId);

        List<Proposal> proposals = proposalRepository.findByRoomId(roomId);

        return proposals.stream()
                .map(ProposalResponse::from)
                .toList();
    }


    /**
     * 제안서 수정
     */
    public ProposalResponse updateProposal(Long proposalId, UpdateProposalRequest request, Long userId) {

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND));

        validateRoomMember(proposal.getRoomId(), userId);

        if (proposal.getStatus() == SubmitStatus.VOTING) {
            throw new BusinessException(ProposalErrorCode.PROPOSAL_LOCKED);
        }

        Long lockOwner = lockService.getLockOwner(proposalId);
        if (lockOwner == null || !lockOwner.equals(userId)) {
            throw new BusinessException(ProposalErrorCode.PROPOSAL_BEING_EDITED);
        }



        ContentFormat contents = ContentFormat.of(
                request.getParagraph(),
                request.getImage(),
                request.getSolution(),
                request.getExpectedEffect()
        );

        proposal.update(request.getTitle(), contents);

        Proposal updated = proposalRepository.save(proposal);

        lockService.renewLock(proposalId, userId);

        return ProposalResponse.from(updated);
    }

    /**
     * 투표 시작
     */
    public ProposalResponse startVoting(Long proposalId, Long userId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND));

        validateRoomMember(proposal.getRoomId(), userId);

        try {
            proposal.startVoting();
        }catch (IllegalStateException e) {
            if (e.getMessage().contains("이미 투표")) {
                throw new BusinessException(ProposalErrorCode.ALREADY_VOTING);
            } else if (e.getMessage().contains("제출 가능한")) {
                throw new BusinessException(ProposalErrorCode.ALREADY_SUBMITTABLE);
            }
        }

        Proposal saved = proposalRepository.save(proposal);

        return ProposalResponse.from(saved);
    }

    /**
     * 투표 종료
     */
    public ProposalResponse endVoting(Long proposalId, Long userId) {

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND));

        validateRoomMember(proposal.getRoomId(), userId);

        try {
            proposal.endVoting();
        }catch (IllegalStateException e) {
            throw new BusinessException(ProposalErrorCode.NOT_IN_VOTING);
        }

        Proposal saved = proposalRepository.save(proposal);

        return ProposalResponse.from(saved);
    }


    /**
     * 해당 제안서에 동의하기
     */
    public void consentProposal(Long proposalId, Long userId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND));

        validateRoomMember(proposal.getRoomId(), userId);

        proposal.checkAndUpdateVotingStatus();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        try {
            proposal.addConsent(new Consenter(user.getId(), user.getNickname()));
            proposalRepository.save(proposal);
        }catch (IllegalStateException e) {
            if (e.getMessage().contains("이미 동의")) {
                throw new BusinessException(ProposalErrorCode.ALREADY_CONSENTED);
            } else if (e.getMessage().contains("투표 중")) {
                throw new BusinessException(ProposalErrorCode.NOT_IN_VOTING);
            } else if (e.getMessage().contains("마감")) {
                throw new BusinessException(ProposalErrorCode.VOTING_DEADLINE_EXPIRED);
            }
        }
    }

    /**
     * 제안서 동의자 목록 조회
     */
    @Transactional(readOnly = true)
    public ConsenterListResponse getConsenters(Long proposalId, Long userId) {

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND));

        validateRoomMember(proposal.getRoomId(), userId);

        return ConsenterListResponse.from(proposal);
    }


    /**
     * 제안서 편집 시작 ( 락 획득 )
     */
    public ProposalResponse startEditing(Long proposalId, Long userId) {

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND));

        validateRoomMember(proposal.getRoomId(), userId);

        if(proposal.getStatus() == SubmitStatus.VOTING) {
            throw new BusinessException(ProposalErrorCode.PROPOSAL_LOCKED);
        }

        if (!lockService.tryLock(proposalId, userId)) {
            throw new BusinessException(ProposalErrorCode.PROPOSAL_BEING_EDITED);
        }

        return ProposalResponse.from(proposal);
    }


    /**
     * 제안서 수정 완료 ( 락 해제 )
     */
    public void finishEditing(Long proposalId, Long userId) {

        proposalRepository.findById(proposalId)
                        .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND));

        validateRoomMember(proposalId, userId);

        lockService.unlock(proposalId, userId);
    }


    @Transactional(readOnly = true)
    public LockStatusResponse getLockStatus(Long proposalId, Long userId) {

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND));

        validateRoomMember(proposal.getRoomId(), userId);

        Long lockOwner = lockService.getLockOwner(proposalId);

        if (lockOwner == null) {
            return LockStatusResponse.unlocked();
        }

        User user = userRepository.findById(lockOwner)
                .orElse(null);

        String nickname = user != null ? user.getNickname() : "알 수 없음";

        return LockStatusResponse.locked(lockOwner, nickname);
    }

    private void validateRoomMember(Long roomId, Long userId) {
        boolean isMember = memberRepository.existsByUserIdAndRoomId(userId, roomId);
        if(!isMember) {
            throw new BusinessException(ProposalErrorCode.UNAUTHORIZED_ACCESS);
        }
    }
}
