package com.dongbang.auth.infrastructure.config;

import com.dongbang.auth.infrastructure.token.JwtTokenService;
import com.dongbang.global.security.JwtAuthenticationFilter;
import com.dongbang.user.application.facade.UserAccountFacade;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfiguration {

    @Bean
    RestClient.Builder oauthRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            UserAccountFacade userAccountFacade
    ) {
        return new JwtAuthenticationFilter(jwtTokenService, userAccountFacade);
    }
}
