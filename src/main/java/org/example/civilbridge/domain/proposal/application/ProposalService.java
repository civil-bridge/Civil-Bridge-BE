package org.example.civilbridge.domain.proposal.application;

import lombok.RequiredArgsConstructor;
import org.example.civilbridge.common.exception.BusinessException;
import org.example.civilbridge.domain.discussionRoom.domain.repository.MemberRepository;
import org.example.civilbridge.domain.proposal.api.dto.*;
import org.example.civilbridge.domain.proposal.domain.model.Consenter;
import org.example.civilbridge.domain.proposal.domain.model.ContentFormat;
import org.example.civilbridge.domain.proposal.domain.model.Proposal;
import org.example.civilbridge.domain.proposal.domain.model.SubmitStatus;
import org.example.civilbridge.domain.proposal.domain.repository.ProposalRepository;
import org.example.civilbridge.domain.proposal.exception.ProposalErrorCode;
import org.example.civilbridge.domain.user.domain.model.User;
import org.example.civilbridge.domain.user.domain.repository.UserRepository;
import org.example.civilbridge.domain.user.exception.UserErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
     * 제안서 작성 (빈 제안서 생성 후 즉시 편집 락 부여)
     */
    public ProposalResponse createProposal(CreateProposalRequest request, Long userId) {

        validateRoomMember(request.getRoomId(), userId);

        if (proposalRepository.countByRoomId(request.getRoomId()) >= 5) {
            throw new BusinessException(ProposalErrorCode.PROPOSAL_LIMIT_EXCEEDED);
        }

        Proposal saved = proposalRepository.save(Proposal.createBlank(request.getRoomId(), userId));

        // 생성자가 즉시 편집 가능하도록 락 자동 획득
        lockService.tryLock(saved.getId(), userId);

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
     * 제안서 수정 (auto-save: @Version 체크 없이 직접 업데이트)
     */
    public ProposalResponse updateProposal(Long proposalId, UpdateProposalRequest request, Long userId) {

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND));

        validateRoomMember(proposal.getRoomId(), userId);

        if (proposal.getStatus() == SubmitStatus.VOTING) {
            throw new BusinessException(ProposalErrorCode.PROPOSAL_LOCKED);
        }

        Long lockOwner = lockService.getLockOwner(proposalId);
        if (lockOwner == null) {
            throw new BusinessException(ProposalErrorCode.LOCK_NOT_ACQUIRED);
        }
        if (!lockOwner.equals(userId)) {
            throw new BusinessException(ProposalErrorCode.PROPOSAL_BEING_EDITED);
        }

        // Auto-save: null 허용 (미완성 내용도 저장 가능)
        ContentFormat contents = ContentFormat.ofNullable(
                request.getParagraph(),
                request.getImage(),
                request.getSolution(),
                request.getExpectedEffect()
        );

        // @Version 체크를 우회하는 직접 업데이트 (Redis 락이 단일 작성자를 이미 보장)
        proposalRepository.updateContent(proposalId, request.getTitle(), contents);
        lockService.renewLock(proposalId, userId);

        return ProposalResponse.from(
                proposalRepository.findById(proposalId)
                        .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND))
        );
    }

    /**
     * 투표 시작 (최종 제출)
     * - 마지막 내용 저장과 투표 상태 전환을 단일 native SQL로 원자적 처리
     * - @Version 체크를 우회하여 PUT(update) 직후 POST(start-voting) 연속 호출 시 발생하는
     *   OptimisticLockingFailureException 방지
     */
    public ProposalResponse startVoting(Long proposalId, SubmitProposalRequest request, Long userId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND));

        validateRoomMember(proposal.getRoomId(), userId);

        if (proposal.getStatus() == SubmitStatus.VOTING) {
            throw new BusinessException(ProposalErrorCode.ALREADY_VOTING);
        }
        if (proposal.getStatus() == SubmitStatus.COMPLETED) {
            throw new BusinessException(ProposalErrorCode.ALREADY_COMPLETED);
        }

        if (!proposal.getAuthorId().equals(userId)) {
            throw new BusinessException(ProposalErrorCode.UNAUTHORIZED_ACCESS);
        }

        Long lockOwner = lockService.getLockOwner(proposalId);
        if (lockOwner != null && !lockOwner.equals(userId)) {
            throw new BusinessException(ProposalErrorCode.PROPOSAL_BEING_EDITED);
        }

        ContentFormat contents = ContentFormat.of(
                request.getParagraph(),
                request.getImage(),
                request.getSolution(),
                request.getExpectedEffect()
        );

        LocalDateTime deadline = request.getDeadline();

        // content 저장 + 투표 전환을 단일 쿼리로 원자적 처리 (@Version 우회)
        proposalRepository.submitAndStartVoting(proposalId, request.getTitle(), contents, deadline, request.getMinAgreements());

        lockService.unlock(proposalId, userId);

        return ProposalResponse.from(
                proposalRepository.findById(proposalId)
                        .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND))
        );
    }

    /**
     * 투표 종료
     * - DB에서 최신 상태를 다시 조회한 뒤 검증
     * - @Version 우회 native query로 상태 확정 (addConsent와의 낙관적 락 충돌 방지)
     */
    public ProposalResponse endVoting(Long proposalId, Long userId) {

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND));

        validateRoomMember(proposal.getRoomId(), userId);

        if (proposal.getStatus() != SubmitStatus.VOTING) {
            throw new BusinessException(ProposalErrorCode.NOT_IN_VOTING);
        }

        if (!proposal.getAuthorId().equals(userId)) {
            throw new BusinessException(ProposalErrorCode.UNAUTHORIZED_ACCESS);
        }

        int consentCount = proposal.getConsents() != null ? proposal.getConsents().size() : 0;
        boolean deadlineExpired = proposal.getDeadline() != null
                && LocalDateTime.now().isAfter(proposal.getDeadline());

        SubmitStatus finalStatus;
        if (consentCount >= proposal.getRequiredConsents()) {
            finalStatus = SubmitStatus.COMPLETED;
        } else if (deadlineExpired) {
            finalStatus = SubmitStatus.REJECTED;
        } else {
            throw new BusinessException(ProposalErrorCode.INSUFFICIENT_CONSENTS);
        }

        // @Version 우회 native query로 투표 결과 확정 (addConsent와의 낙관적 락 충돌 방지)
        proposalRepository.updateVotingResult(proposalId, finalStatus);

        return ProposalResponse.from(
                proposalRepository.findById(proposalId)
                        .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND))
        );
    }


    /**
     * 해당 제안서에 동의하기
     */
    public ConsentResponse consentProposal(Long proposalId, Long userId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND));

        validateRoomMember(proposal.getRoomId(), userId);

        if (proposal.getStatus() != SubmitStatus.VOTING) {
            throw new BusinessException(ProposalErrorCode.NOT_IN_VOTING);
        }

        if (proposal.getDeadline() != null && LocalDateTime.now().isAfter(proposal.getDeadline())) {
            throw new BusinessException(ProposalErrorCode.VOTING_DEADLINE_EXPIRED);
        }

        if (proposal.getConsents() != null &&
                proposal.getConsents().stream().anyMatch(c -> c.getId().equals(userId))) {
            throw new BusinessException(ProposalErrorCode.ALREADY_CONSENTED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // @Version 우회 native query로 consents 컬럼에만 append (스케줄러와의 낙관적 락 충돌 방지)
        proposalRepository.addConsent(proposalId, new Consenter(user.getId(), user.getNickname()));

        // clearAutomatically = true 덕분에 JPA 캐시가 비워져 DB의 최신 consents 반영
        Proposal updated = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND));

        int totalConsents = updated.getConsents() != null ? updated.getConsents().size() : 0;
        return new ConsentResponse(totalConsents);
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

        Proposal proposal = proposalRepository.findById(proposalId)
                        .orElseThrow(() -> new BusinessException(ProposalErrorCode.PROPOSAL_NOT_FOUND));

        validateRoomMember(proposal.getRoomId(), userId);

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
