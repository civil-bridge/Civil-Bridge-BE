package org.example.gyeonggi_partners.domain.user.api.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Schema(description = "로그인 요청")
@AllArgsConstructor
@NoArgsConstructor
public class SignInRequest {

    @NotBlank(message = "로그인 ID는 필수입니다")
    @Schema(description = "로그인 ID", example = "newuser123")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Schema(description = "비밀번호", example = "password123!")
    private String password;

}
