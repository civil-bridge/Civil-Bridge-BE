package org.example.gyeonggi_partners.common.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

/**
 * JWT 관련 설정값을 관리하는 Properties 클래스
 * application.properties 또는 환경별 properties 파일에서 값을 읽어옵니다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 서명에 사용할 Secret Key
     * 최소 256비트(32자) 이상 권장
     */
    private String secret;

    /**
     * Access Token 유효기간 (밀리초)
     * 기본값: 3600000ms = 1시간
     */
    private Long accessTokenExpiration;


    private Long refreshTokenExpiration;
}