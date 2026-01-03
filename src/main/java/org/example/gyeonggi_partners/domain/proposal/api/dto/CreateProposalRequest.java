package org.example.gyeonggi_partners.domain.proposal.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "제안서 생성 요청")
public class CreateProposalRequest {

    private String title;

    private String paragraph;

    private String image;

    private String solution;

    private String expectedEffect;

    private Long roomId;
}
