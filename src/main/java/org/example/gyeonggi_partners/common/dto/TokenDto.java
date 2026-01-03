package org.example.gyeonggi_partners.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class TokenDto {

    private final String grantType;
    private final String accessToken;
    private final String refreshToken;
}