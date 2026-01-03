package org.example.gyeonggi_partners.domain.proposal.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.gyeonggi_partners.domain.proposal.domain.model.Proposal;
import org.example.gyeonggi_partners.domain.proposal.domain.model.SubmitStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ProposalResponse {

    private Long id;
    private Long roomId;
    private Long authorId;

    private String title;
    private ContentFormatDto contents;

    private SubmitStatus status;
    private List<ConsenterDto> consents;
    private LocalDateTime deadline;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProposalResponse from(Proposal proposal) {
        return ProposalResponse.builder()
                .id(proposal.getId())
                .roomId(proposal.getRoomId())
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
