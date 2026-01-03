package org.example.gyeonggi_partners.domain.proposal.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.gyeonggi_partners.domain.proposal.domain.model.ContentFormat;

@Getter
@Builder
@AllArgsConstructor
public class ContentFormatDto {

    private String paragraph;
    private String image;
    private String solution;
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
