package org.example.gyeonggi_partners.domain.user.application;


import lombok.RequiredArgsConstructor;
import org.example.gyeonggi_partners.common.dto.TokenDto;
import org.example.gyeonggi_partners.common.exception.BusinessException;
import org.example.gyeonggi_partners.common.jwt.CustomUserDetails;
import org.example.gyeonggi_partners.common.jwt.JwtTokenProvider;
import org.example.gyeonggi_partners.domain.user.api.dto.SignInRequest;
import org.example.gyeonggi_partners.domain.user.api.dto.SignInResponse;
import org.example.gyeonggi_partners.domain.user.domain.model.User;
import org.example.gyeonggi_partners.domain.user.domain.repository.UserRepository;
import org.example.gyeonggi_partners.domain.user.exception.UserErrorCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private static final String REFRESH_TOKEN_PREFIX = "RT:";

    public SignInResponse login(SignInRequest request) {

        try {
            // 1. 인증 토큰 생성
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            request.getLoginId(),
                            request.getPassword()
                    );

            // 2. Spring Security 인증
            Authentication authentication =
                    authenticationManager.authenticate(authToken);

            // 3. JWT 토큰 생성
            TokenDto tokenDto = jwtTokenProvider.generateTokenDto(authentication);

            // 4. CustomUserDetails 추출
            CustomUserDetails userDetails =
                    (CustomUserDetails) authentication.getPrincipal();

            // 5. Refresh Token을 Redis에 저장
            redisTemplate.opsForValue().set(
                    REFRESH_TOKEN_PREFIX + userDetails.getUserId(),
                    tokenDto.getRefreshToken(),
                    7,
                    TimeUnit.DAYS
            );

            // 6. 응답 생성
            return new SignInResponse(
                    userDetails.getUserId(),
                    userDetails.getNickname(),
                    userDetails.getEmail(),
                    userDetails.getRole(),
                    tokenDto.getGrantType(),
                    tokenDto.getAccessToken(),
                    tokenDto.getRefreshToken()
            );

        } catch (BadCredentialsException | UsernameNotFoundException |
                 InternalAuthenticationServiceException e) {
            // ⭐ Spring Security 예외를 BusinessException으로 변환
            throw new BusinessException(UserErrorCode.LOGIN_FAILED);
        }
    }

    /**
     * 로그아웃
     * Redis에서 Refresh Token을 삭제하여 토큰 재발급 불가능하게 만듦
     */
    public void logoutByUserId(Long userId) {
        // Redis에서 해당 유저의 Refresh Token 삭제
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
    }


    /**
     * Refresh Token으로 Access Token 재발급
     */
    public TokenDto refresh(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(UserErrorCode.INVALID_TOKEN);
        }

        // 2. Refresh Token에서 userId 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // 3. Redis에 저장된 Refresh Token과 비교
        String storedRefreshToken = redisTemplate.opsForValue()
                .get(REFRESH_TOKEN_PREFIX + userId);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(UserErrorCode.INVALID_TOKEN);
        }

        // 4. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // 5. 새로운 Access Token만 생성 (Refresh Token은 재사용)
        UserDetails userDetails = user.toUserDetails();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);

        // 6. TokenDto 반환
        return TokenDto.builder()
                .grantType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // 기존 Refresh Token 그대로
                .build();
    }


}
