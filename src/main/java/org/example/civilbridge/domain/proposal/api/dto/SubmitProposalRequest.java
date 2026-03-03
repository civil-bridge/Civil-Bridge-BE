package org.example.civilbridge.domain.proposal.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "제안서 최종 제출 및 투표 시작 요청")
public class SubmitProposalRequest {

    @NotBlank
    @Size(min = 5, max = 100)
    @Schema(description = "제안서 제목 (5~100자)", example = "수원시 영통구 교통 체증 해결 방안")
    private String title;

    @NotBlank
    @Schema(description = "본문 내용", example = "경기도 수원시 영통구의 교통 체증 문제를 해결하기 위한 제안입니다.")
    private String paragraph;

    @Schema(description = "이미지 URL", example = "https://example.com/images/proposal.png")
    private String image;

    @Schema(description = "해결 방안", example = "버스 전용 차로 확대 및 신호 체계 개선")
    private String solution;

    @Schema(description = "기대 효과", example = "출퇴근 시간 교통 체증 30% 감소 예상")
    private String expectedEffect;
}
