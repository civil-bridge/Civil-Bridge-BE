package org.example.gyeonggi_partners.domain.proposal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gyeonggi_partners.common.dto.ApiResponse;
import org.example.gyeonggi_partners.common.jwt.CustomUserDetails;
import org.example.gyeonggi_partners.domain.proposal.api.dto.*;
import org.example.gyeonggi_partners.domain.proposal.application.ProposalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Proposal", description = "제안서 API")
@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    @Operation(summary = "제안서 생성", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<ApiResponse<ProposalResponse>> createProposal(
            @Valid @RequestBody CreateProposalRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        ProposalResponse response = proposalService.createProposal(request, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.success(response, "제안서 생성 성공"));
    }


    @Operation(summary = "제안서 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{proposalId}")
    public ResponseEntity<ApiResponse<ProposalResponse>> getProposal(
            @PathVariable Long proposalId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        ProposalResponse response = proposalService.getProposal(proposalId, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @Operation(summary = "특정 논의방의 제안서 목록 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<List<ProposalResponse>>> getProposalsByRoom(
            @PathVariable Long roomId,
            @Parameter(required = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<ProposalResponse> proposals = proposalService.getProposalsByRoom(roomId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(proposals));
    }


    @Operation(summary = "제안서 편집 시작 (락 획득)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{proposalId}/start-editing")
    public ResponseEntity<ApiResponse<Void>> startEditing(
            @PathVariable Long proposalId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        ProposalResponse response = proposalService.startEditing(proposalId, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.success(null, "편집 시작."));
    }


    @Operation(summary = "제안서 편집 완료 (락 해제)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{proposalId}/finish-editing")
    public ResponseEntity<ApiResponse<Void>> finishEditing(
            @PathVariable Long proposalId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        proposalService.finishEditing(proposalId, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.success(null, "편집 완료."));
    }

    @Operation(summary = "제안서 수정", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{proposalId}")
    public ResponseEntity<ApiResponse<ProposalResponse>> updateProposal(
            @PathVariable Long proposalId,
            @Valid @RequestBody UpdateProposalRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        ProposalResponse response = proposalService.updateProposal(proposalId, request, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.success(response, "제안서 수정 성공"));
    }

    @Operation(summary = "제안서 락 상태 확인", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{proposalId}/lock-status")
    public ResponseEntity<ApiResponse<LockStatusResponse>> getLockStatus(
            @PathVariable Long proposalId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        LockStatusResponse response = proposalService.getLockStatus(proposalId, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @Operation(summary = "투표 시작", description = "기본 투표 기간 = 3일", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{proposalId}/start-voting")
    public ResponseEntity<ApiResponse<ProposalResponse>> startVoting(
            @PathVariable Long proposalId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ProposalResponse response = proposalService.startVoting(proposalId, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.success(response, "투표 시작."));
    }


    @Operation(summary = "투표 종료 (수동 종료)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{proposalId}/end-voting")
    public ResponseEntity<ApiResponse<ProposalResponse>> endVoting(
            @PathVariable Long proposalId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        ProposalResponse response = proposalService.endVoting(proposalId, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.success(response, "투표 종료."));
    }



    @Operation(summary = "제안서 동의", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{proposalId}/consents")
    public ResponseEntity<ApiResponse<ProposalResponse>> consentProposal(
            @PathVariable Long proposalId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        proposalService.consentProposal(proposalId, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.success(null, "제안서 동의 완료"));
    }


    @Operation(summary = "제안서 동의자 목록 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{proposalId}/consenters")
    public ResponseEntity<ApiResponse<ConsenterListResponse>> getConsenters(
            @PathVariable Long proposalId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        ConsenterListResponse response = proposalService.getConsenters(proposalId, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
