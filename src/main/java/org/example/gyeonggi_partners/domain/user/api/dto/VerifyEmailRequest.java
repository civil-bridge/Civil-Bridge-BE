package org.example.gyeonggi_partners.domain.user.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이메일 인증번호 검증 요청 DTO
 */
@Getter
@NoArgsConstructor
public class VerifyEmailRequest {

    private String email;
    private String code;
}
