package org.example.civilbridge.domain.proposal.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.civilbridge.domain.proposal.domain.model.Proposal;
import org.example.civilbridge.domain.proposal.domain.model.SubmitStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "제안서 응답")
public class ProposalResponse {

    @Schema(description = "제안서 ID", example = "1")
    private Long id;

    @Schema(description = "토론방 ID", example = "1")
    private Long roomId;

    @Schema(description = "작성자 ID", example = "1")
    private Long authorId;

    @Schema(description = "제안서 제목", example = "수원시 영통구 교통 체증 해결 방안")
    private String title;

    @Schema(description = "제안서 본문")
    private ContentFormatDto contents;

    @Schema(description = "제안서 상태", example = "DRAFT")
    private SubmitStatus status;

    @Schema(description = "동의자 목록")
    private List<ConsenterDto> consents;

    @Schema(description = "마감 기한", example = "2026-03-01T00:00:00")
    private LocalDateTime deadline;

    @Schema(description = "생성일시", example = "2026-02-17T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2026-02-17T10:00:00")
    private LocalDateTime updatedAt;

    public static ProposalResponse from(Proposal proposal) {
        return ProposalResponse.builder()
                .id(proposal.getId())
                .roomId(proposal.getRoomId())
                .authorId(proposal.getAuthorId())
                .title(proposal.getTitle())
                .contents(ContentFormatDto.from(proposal.getContents()))
                .status(proposal.getStatus())
                .consents(proposal.getConsents() != null
                ? proposal.getConsents().stream()
                        .map(ConsenterDto::from)
                        .toList()
                        : List.of())
                .deadline(proposal.getDeadline())
                .createdAt(proposal.getCreatedAt())
                .updatedAt(proposal.getUpdatedAt())
                .build();

    }
}
