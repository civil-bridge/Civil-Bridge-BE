package org.example.civilbridge.domain.proposal.domain.model;

import lombok.Getter;

@Getter
public class ContentFormat {

    // 내용
    private String paragraph;

    // 이미지
    private String image;

    // 해결방안
    private String solution;

    // 기대효과
    private String expectedEffect;


    private ContentFormat() {
        // Jackson JSON 역직렬화용
    }

    private ContentFormat(String paragraph, String image, String solution, String expectedEffect) {
        validateContentFormat(paragraph);

        this.paragraph = paragraph;
        this.image = image;
        this.solution = solution;
        this.expectedEffect = expectedEffect;
    }

    private ContentFormat(String paragraph, String image, String solution, String expectedEffect, boolean skipValidation) {
        this.paragraph = paragraph;
        this.image = image;
        this.solution = solution;
        this.expectedEffect = expectedEffect;
    }


    public static ContentFormat of(String paragraph, String image, String solution, String expectedEffect) {
        return new ContentFormat(paragraph, image, solution, expectedEffect);
    }

    // Auto-save용: paragraph 필수 검증 없이 저장 (미완성 내용 허용)
    public static ContentFormat ofNullable(String paragraph, String image, String solution, String expectedEffect) {
        return new ContentFormat(paragraph, image, solution, expectedEffect, true);
    }


    private void validateContentFormat(String paragraph) {
        if (paragraph == null || paragraph.trim().isEmpty()) {
            throw new IllegalArgumentException("제안서 내용은 필수입니다.");
        }
    }
}

