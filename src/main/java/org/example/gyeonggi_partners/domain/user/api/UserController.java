package org.example.gyeonggi_partners.domain.user.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.gyeonggi_partners.common.dto.ApiResponse;
import org.example.gyeonggi_partners.domain.user.api.dto.EmailVerificationRequest;
import org.example.gyeonggi_partners.domain.user.api.dto.SignUpRequest;
import org.example.gyeonggi_partners.domain.user.api.dto.SignUpResponse;
import org.example.gyeonggi_partners.domain.user.api.dto.VerifyEmailRequest;
import org.example.gyeonggi_partners.domain.user.application.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * User API Controller
 * 회원가입, 이메일 인증 등
 */
@Tag(name = "User", description = "회원가입 및 회원관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원가입
     * POST /api/users/signup
     */
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(@RequestBody SignUpRequest request) {
        SignUpResponse response = userService.signUp(request);
        return ResponseEntity.ok(ApiResponse.success(response, "회원가입에 성공했습니다."));
    }

    /**
     * 이메일 인증번호 발송
     * POST /api/users/email/send
     */
    @Operation(summary = "이메일 인증번호 발송", description = "입력한 이메일로 6자리 인증번호를 발송합니다. (유효시간: 5분)")
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendEmailVerification(@RequestBody EmailVerificationRequest request) {
        String email = request.getEmail();
        userService.sendEmailVerification(email);
        return ResponseEntity.ok(ApiResponse.success(null, "인증번호가 성공적으로 발송되었습니다."));
    }

    /**
     * 이메일 인증번호 검증
     * POST /api/users/email/verify
     */
    @Operation(summary = "이메일 인증번호 검증", description = "발송된 인증번호를 검증합니다.")
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestBody VerifyEmailRequest request) {
        userService.verifyEmail(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(null, "이메일 인증에 성공했습니다."));
    }
}
