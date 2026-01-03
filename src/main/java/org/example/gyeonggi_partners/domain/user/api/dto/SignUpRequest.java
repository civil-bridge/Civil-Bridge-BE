package org.example.gyeonggi_partners.domain.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청 DTO
 */
@Getter
@NoArgsConstructor
@Schema(description = "회원가입 요청")
public class SignUpRequest {

    @NotBlank(message = "로그인 ID는 필수입니다")
    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$",
             message = "로그인 ID는 6-20자의 영문, 숫자만 사용 가능합니다")
    @Size(min = 6, max = 20)
    @Schema(description = "로그인 ID", example = "newuser123")
    private String loginId;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
             message = "비밀번호는 8자 이상, 영문, 숫자, 특수문자(@$!%*#?&)를 포함해야 합니다")
    @Size(min = 8 , max = 50)
    @Schema(description = "비밀번호", example = "password123!")
    private String password;
    
    @NotBlank(message = "이름은 필수입니다")
    @Pattern(regexp = "^[가-힣a-zA-Z]{2,20}$",
             message = "이름은 2-20자의 한글 또는 영문만 사용 가능합니다")
    @Size(min =2, max = 20)
    @Schema(description = "이름", example = "홍길동")
    private String name;
    
    @NotBlank(message = "닉네임은 필수입니다")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]{2,15}$",
             message = "닉네임은 15자 이내로 입력해야 합니다")
    @Size(max = 15)
    @Schema(description = "닉네임", example = "길동이")
    private String nickname;
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,30}$",
             message = "이메일은 50자를 초과할 수 없습니다")
    @Size(max = 50)
    @Schema(description = "이메일", example = "hong@example.com")
    private String email;
    
    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$",
            message = "올바른 휴대폰 번호 형식이 아닙니다 (예: 010-1234-5678)")
    @Size(max = 20)
    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phoneNumber;
}
