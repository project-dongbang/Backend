package com.dongbang.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String issuer,
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        Duration oauthStateTtl,
        boolean cookieSecure,
        List<String> allowedRedirectUris,
        Provider google,
        Provider kakao
) {
    public record Provider(
            String clientId,
            String clientSecret,
            String callbackUri,
            String authorizationUri,
            String tokenUri,
            String userInfoUri,
            String scope
    ) {
        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank()
                    && callbackUri != null && !callbackUri.isBlank();
        }
    }
}
