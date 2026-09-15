package com.dongbang.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
                .version("v1"));
    }
}
