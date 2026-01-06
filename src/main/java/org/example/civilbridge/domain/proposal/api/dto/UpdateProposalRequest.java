package org.example.civilbridge.domain.proposal.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProposalRequest {

    private String title;

    private String paragraph;

    private String image;

    private String solution;

    private String expectedEffect;
}
