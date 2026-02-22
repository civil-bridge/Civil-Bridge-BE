package org.example.civilbridge.domain.proposal.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.civilbridge.domain.proposal.domain.model.ContentFormat;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "제안서 본문 형식")
public class ContentFormatDto {

    @Schema(description = "본문 내용", example = "경기도 수원시 영통구의 교통 체증 문제를 해결하기 위한 제안입니다.")
    private String paragraph;

    @Schema(description = "이미지 URL", example = "https://example.com/images/proposal.png")
    private String image;

    @Schema(description = "해결 방안", example = "버스 전용 차로 확대 및 신호 체계 개선")
    private String solution;

    @Schema(description = "기대 효과", example = "출퇴근 시간 교통 체증 30% 감소 예상")
    private String expectedEffect;

    public static ContentFormatDto from(ContentFormat content) {
        return ContentFormatDto.builder()
                .paragraph(content.getParagraph())
                .image(content.getImage())
                .solution(content.getSolution())
                .expectedEffect(content.getExpectedEffect())
                .build();
    }
}
