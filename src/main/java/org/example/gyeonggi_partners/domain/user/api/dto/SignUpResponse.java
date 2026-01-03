package org.example.gyeonggi_partners.domain.user.api.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 회원가입 응답 DTO
 */
@Getter
@NoArgsConstructor
public class SignUpResponse {

    private Long userId;
    private String nickname;
    private String email;
    private String role;

    public SignUpResponse(Long userId, String nickname, String email, String role) {
        this.userId = userId;
        this.nickname = nickname;
        this.email = email;
        this.role = role;
    }
}