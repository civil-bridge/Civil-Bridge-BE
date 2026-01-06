package org.example.civilbridge.domain.proposal.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.civilbridge.domain.proposal.domain.model.Consenter;

@Getter
@AllArgsConstructor
@Builder
public class ConsenterDto {

    private Long id;
    private String nickname;

    public static ConsenterDto from(Consenter consenter) {
        return ConsenterDto.builder()
                .id(consenter.getId())
                .nickname(consenter.getNickname())
                .build();
    }
}
