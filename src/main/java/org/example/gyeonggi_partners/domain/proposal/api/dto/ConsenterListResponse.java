package org.example.gyeonggi_partners.domain.proposal.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.gyeonggi_partners.domain.proposal.domain.model.Proposal;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class ConsenterListResponse {

    private int totalConsents;
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
