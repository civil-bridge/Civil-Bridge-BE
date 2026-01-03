package org.example.gyeonggi_partners.config;

import lombok.RequiredArgsConstructor;
import org.example.gyeonggi_partners.common.jwt.JwtAuthenticationFilter;
import org.example.gyeonggi_partners.common.jwt.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// CORS 관련 라이브러리 임포트
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@Profile("!prod") // 운영(prod) 프로파일이 아닐 때만 이 설정을 사용
@RequiredArgsConstructor
public class SecurityConfigDev {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 1. 기본 인증 방식 비활성화
        http.csrf(AbstractHttpConfigurer::disable) // CSRF 보호 비활성화
                .formLogin(AbstractHttpConfigurer::disable) // Form 기반 로그인 비활성화
                .httpBasic(AbstractHttpConfigurer::disable); // HTTP Basic 인증 비활성화

        // 2. CORS 설정 추가 (개발 환경에서는 모든 origin 허용)
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // 3. 세션 정책을 STATELESS로 설정 (JWT는 세션을 사용하지 않음)
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 4. JWT 필터 추가
        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class
        );

        // HTTP 요청에 대한 인가 규칙 설정
        http.authorizeHttpRequests(auth -> auth
                // Swagger UI 및 API 문서 관련 경로 모두 허용
                .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                ).permitAll()

                // 로그인은 인증 불필요
                .requestMatchers("/api/auth/login").permitAll()
                
                // 로그아웃은 인증 필요
                .requestMatchers("/api/auth/logout").authenticated()

                //논의방 관련은 인증필요
                .requestMatchers("/api/discussion-rooms/**").authenticated()
                
                // Actuator 헬스 체크 경로 허용
                .requestMatchers("/actuator/health/**").permitAll()

                // 개발 환경에서는 나머지 모든 요청을 허용하여 빠른 테스트를 지원
                .anyRequest().permitAll()
        );

        return http.build();
    }

    /**
     * 개발 환경용 CORS 설정
     * 모든 origin, 모든 HTTP 메서드, 모든 헤더를 허용
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 모든 origin 허용 (개발 환경)
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // 모든 HTTP 메서드 허용
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // 모든 헤더 허용
        configuration.setAllowedHeaders(List.of("*"));
        
        // 인증 정보 포함 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);
        
        // preflight 요청 캐싱 시간 (1시간)
        configuration.setMaxAge(3600L);
        
        // 노출할 헤더 설정 (클라이언트에서 접근 가능한 헤더)
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder Bean
     * BCrypt 해싱 알고리즘 사용
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

}
