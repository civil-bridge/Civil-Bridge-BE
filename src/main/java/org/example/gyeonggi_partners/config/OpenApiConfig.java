package org.example.gyeonggi_partners.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        // Security Scheme 이름
        String jwtSchemeName = "bearerAuth";
        
        // Security Scheme 정의
        // HTTP Bearer 방식으로 설정하면 Swagger가 자동으로 "Bearer " 접두사를 붙여줌
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
        
        return new OpenAPI()
                .info(new Info()
                        .title("경기 파트너스 API")
                        .description("경기 파트너스 백엔드 API 문서")
                        .version("v1.0.0"))
                .components(new Components().addSecuritySchemes(jwtSchemeName, securityScheme));
    }
}
