package org.example.civilbridge.domain.proposal.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "제안서 동의 응답")
public class ConsentResponse {

    @Schema(description = "동의 후 총 동의 인원수", example = "6")
    private int totalConsents;
}
