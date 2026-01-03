package org.example.gyeonggi_partners.common.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.example.gyeonggi_partners.common.dto.TokenDto;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "role";
    private static final String BEARER_TYPE = "Bearer";

    private final Key key;
    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 인증(Authentication) 객체를 기반으로 Access Token과 Refresh Token을 생성합니다.
     * 로그인을 할떄 호출됩니다.
     */
    public TokenDto generateTokenDto(Authentication authentication) {
        // CustomUserDetails에서 userId 추출
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        
        // 권한 정보들을 쉼표(,)로 구분된 문자열로 변환
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();

        // Access Token 생성 (userId 포함!)
        Date accessTokenExpiresIn = new Date(now + jwtProperties.getAccessTokenExpiration());
        String accessToken = Jwts.builder()
                .subject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .claim("userId", userDetails.getUserId())  // userId 추가!
                .expiration(accessTokenExpiresIn)
                .signWith(key)
                .compact();

        // Refresh Token (userId 추가!)
        String refreshToken = Jwts.builder()
                .subject(String.valueOf(userDetails.getUserId())) // userId 추가
                .expiration(new Date(now + jwtProperties.getRefreshTokenExpiration()))
                .signWith(key)
                .compact();



        return TokenDto.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    //=========================로그인 이후 API에 접근할때  호출되는 함수 ==================================

    /**
     * JWT 토큰을 복호화하여 토큰에 들어있는 정보를 꺼내 Authentication 객체로 반환합니다.
     *
     */
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();

        // JWT에서 userId 추출
        Long userId = claims.get("userId", Long.class);
        
        // CustomUserDetails 객체 생성
        // nickname, email, role은 JWT에 없으므로 null 또는 기본값
        CustomUserDetails principal = new CustomUserDetails(
                userId,
                null,  // nickname
                null,  // email
                claims.get(AUTHORITIES_KEY).toString().replace("ROLE_", ""),  // role
                claims.getSubject(),  // loginId
                "",  // password
                authorities
        );
        
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    /**
     * 토큰의 유효성을 검증합니다.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith((SecretKey) key).build().parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    /**
     * 토큰에서 Claims 정보를 추출합니다.
     */
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 토큰이 만료되었더라도 정보를 꺼내기 위해 Claims를 반환
            return e.getClaims();
        }
    }

    // userId 추출 메서드 추가
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Access Token만 생성 (Refresh 시 사용)
     */
    public String generateAccessToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .claim("userId", userDetails.getUserId())
                .expiration(accessTokenExpiresIn)
                .signWith(key)
                .compact();
    }


}