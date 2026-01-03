package org.example.gyeonggi_partners.domain.proposal.domain.model;

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


    private ContentFormat(String paragraph, String image, String solution, String expectedEffect) {
        validateContentFormat(paragraph);

        this.paragraph = paragraph;
        this.image = image;
        this.solution = solution;
        this.expectedEffect = expectedEffect;
    }


    public static ContentFormat of(String paragraph, String image, String solution, String expectedEffect) {
        return new ContentFormat(paragraph, image, solution, expectedEffect);
    }


    private void validateContentFormat(String paragraph) {
        if (paragraph == null || paragraph.trim().isEmpty()) {
            throw new IllegalArgumentException("제안서 내용은 필수입니다.");
        }
    }
}

