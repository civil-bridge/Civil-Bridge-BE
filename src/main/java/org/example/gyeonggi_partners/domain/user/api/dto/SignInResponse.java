package org.example.gyeonggi_partners.domain.user.api.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignInResponse {

    // === 사용자 정보 ===
    private Long userId;
    private String nickname;
    private String email;
    private String role;

    // === 토큰 정보 ===
    private String grantType;
    private String accessToken;
    private String refreshToken;

}