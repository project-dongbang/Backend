package com.dongbang.auth.infrastructure.web;

import com.dongbang.auth.application.token.TokenPair;
import com.dongbang.auth.domain.OAuthProvider;
import com.dongbang.auth.infrastructure.config.AuthProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class AuthCookieService {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final AuthProperties properties;

    public AuthCookieService(AuthProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie accessToken(TokenPair tokens) {
        return secureCookie(
                ACCESS_TOKEN_COOKIE,
                tokens.accessToken(),
                "/",
                remaining(tokens.accessTokenExpiresAt())
        );
    }

    public ResponseCookie refreshToken(TokenPair tokens) {
        return secureCookie(
                REFRESH_TOKEN_COOKIE,
                tokens.refreshToken(),
                "/api/v1/auth",
                remaining(tokens.refreshTokenExpiresAt())
        );
    }

    public ResponseCookie oauthState(OAuthProvider provider, String stateToken) {
        return secureCookie(
                stateCookieName(provider),
                stateToken,
                callbackPath(provider),
                properties.oauthStateTtl()
        );
    }

    public ResponseCookie clearOAuthState(OAuthProvider provider) {
        return expiredCookie(stateCookieName(provider), callbackPath(provider));
    }

    public ResponseCookie clearAccessToken() {
        return expiredCookie(ACCESS_TOKEN_COOKIE, "/");
    }

    public ResponseCookie clearRefreshToken() {
        return expiredCookie(REFRESH_TOKEN_COOKIE, "/api/v1/auth");
    }

    public String stateCookieName(OAuthProvider provider) {
        return "oauth_state_" + provider.name().toLowerCase();
    }

    private ResponseCookie secureCookie(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSecure() ? "None" : "Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie expiredCookie(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSecure() ? "None" : "Lax")
                .path(path)
                .maxAge(Duration.ZERO)
                .build();
    }

    private Duration remaining(Instant expiresAt) {
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private String callbackPath(OAuthProvider provider) {
        return "/api/v1/auth/oauth/" + provider.name().toLowerCase() + "/callback";
    }
}
