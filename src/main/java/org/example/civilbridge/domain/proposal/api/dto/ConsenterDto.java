package org.example.civilbridge.domain.proposal.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.civilbridge.domain.proposal.domain.model.Consenter;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "동의자 정보")
public class ConsenterDto {

    @Schema(description = "동의자 ID", example = "1")
    private Long id;

    @Schema(description = "동의자 닉네임", example = "홍길동")
    private String nickname;

    public static ConsenterDto from(Consenter consenter) {
        return ConsenterDto.builder()
                .id(consenter.getId())
                .nickname(consenter.getNickname())
                .build();
    }
}
