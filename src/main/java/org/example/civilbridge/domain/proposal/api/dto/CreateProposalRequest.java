package org.example.civilbridge.domain.proposal.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "제안서 생성 요청")
public class CreateProposalRequest {

    @NotNull(message = "논의방 ID는 필수입니다.")
    @Schema(description = "토론방 ID", example = "1")
    private Long roomId;
}
