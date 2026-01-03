package org.example.gyeonggi_partners.domain.user.api.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class EmailVerificationRequest {
    @Schema(description = "인증번호를 받을 이메일", example = "user@example.com")
    String email;
}
