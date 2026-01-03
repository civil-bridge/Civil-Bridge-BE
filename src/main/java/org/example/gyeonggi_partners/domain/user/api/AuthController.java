package org.example.gyeonggi_partners.domain.user.api;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gyeonggi_partners.common.dto.ApiResponse;
import org.example.gyeonggi_partners.common.dto.TokenDto;
import org.example.gyeonggi_partners.common.jwt.CustomUserDetails;
import org.example.gyeonggi_partners.domain.user.api.dto.RefreshTokenRequest;
import org.example.gyeonggi_partners.domain.user.api.dto.SignInRequest;
import org.example.gyeonggi_partners.domain.user.api.dto.SignInResponse;
import org.example.gyeonggi_partners.domain.user.application.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "사용자 인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "로그인",
            description = "사용자 로그인을 수행하고 JWT 토큰을 발급합니다."
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SignInResponse>> login(
            @Valid @RequestBody SignInRequest request) {

        SignInResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(response,"로그인에 성공했습니다.")
        );
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 로그인된 사용자를 로그아웃하고 Refresh Token을 무효화합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        authService.logoutByUserId(userDetails.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success(null, "로그아웃되었습니다.")
        );
    }

    @Operation(
            summary = "Access Token 재발급",
            description = "Refresh Token으로 새로운 Access Token을 발급받습니다."
    )
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenDto>> refresh(
            @RequestBody RefreshTokenRequest request) {

        TokenDto tokenDto = authService.refresh(request.getRefreshToken());

        return ResponseEntity.ok(
                ApiResponse.success(tokenDto, "토큰이 갱신되었습니다.")
        );
    }
}
