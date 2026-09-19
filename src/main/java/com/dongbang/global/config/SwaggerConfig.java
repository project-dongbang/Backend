package com.dongbang.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SwaggerConfig {

    @Bean
    OpenAPI dongBangOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("DongBang API")
                .description("동아리 운영 플랫폼 DongBang의 REST API 문서입니다.")
                .version("v1"))
            .components(new Components()
                .addSecuritySchemes("accessCookie", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.COOKIE)
                    .name("access_token")
                    .description("OAuth 로그인 콜백에서 발급되는 HttpOnly Access Token 쿠키"))
                .addSecuritySchemes("csrfHeader", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-XSRF-TOKEN")
                    .description("상태 변경 요청에 필요한 CSRF 헤더"))
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT Bearer 토큰 (Authorization: Bearer <token>)")));
    }
}
