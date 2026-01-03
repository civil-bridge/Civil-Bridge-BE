package org.example.gyeonggi_partners.common.jwt;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증을 위한 Spring Security 필터.
 * 로그인 이후 서버의 요청할때 검증하는 필터*/
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter { // 1. OncePerRequestFilter 상속

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider; // 2. JwtTokenProvider 의존성 주입

    // 3. 필터의 핵심 로직 구현
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 4. 요청 헤더에서 JWT 토큰을 추출
        String jwt = resolveToken(request);

        // 5. 토큰 유효성 검증
        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
            // 6. 토큰이 유효할 경우, 토큰에서 Authentication 객체를 가져와서 SecurityContext에 저장
            Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication); // 7. SecurityContext에 저장
        }

        // 8. 다음 필터 체인으로 요청을 전달
        filterChain.doFilter(request, response);
    }

    /**
     * HttpServletRequest의 헤더에서 Bearer 토큰을 추출하는 private 메서드
     * @param request 들어온 요청
     * @return 추출된 토큰 문자열 (없으면 null)
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken)) {
            // "Bearer "로 시작하면 제거
            if (bearerToken.startsWith(BEARER_PREFIX)) {
                return bearerToken.substring(7);
            }
            // Bearer 없이 토큰만 있는 경우도 허용 (Swagger 호환)
            return bearerToken;
        }
        return null;
    }
}