package org.example.civilbridge.domain.proposal.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.civilbridge.domain.proposal.domain.model.Proposal;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "동의자 목록 응답")
public class ConsenterListResponse {

    @Schema(description = "총 동의 수", example = "5")
    private int totalConsents;

    @Schema(description = "동의자 목록")
    private List<ConsenterDto> consenters;

    public static ConsenterListResponse from(Proposal proposal) {
        List<ConsenterDto> consenterDtos = proposal.getConsents() != null
                ? proposal.getConsents().stream()
                .map(ConsenterDto::from)
                .toList()
                : List.of();

        return ConsenterListResponse.builder()
                .totalConsents(consenterDtos.size())
                .consenters(consenterDtos)
                .build();

    }
}
