package com.dongbang.auth.infrastructure.token;

import com.dongbang.auth.domain.OAuthProvider;
import com.dongbang.auth.exception.AuthErrorCode;
import com.dongbang.auth.infrastructure.config.AuthProperties;
import com.dongbang.global.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties(
                "dongbang-test",
                "dongbang-test-jwt-secret-key-over-32-bytes",
                Duration.ofMinutes(15),
                Duration.ofDays(14),
                Duration.ofMinutes(5),
                false,
                List.of("http://localhost:5173/oauth/callback"),
                null,
                null
        );
        jwtTokenService = new JwtTokenService(properties);
    }

    @Test
    @DisplayName("Access Token과 Refresh Token의 사용자 및 세션 정보를 구분해 검증한다")
    void issueAndParseTokenPair() {
        UUID sessionKey = UUID.randomUUID();

        var tokens = jwtTokenService.issueTokenPair(7L, sessionKey);

        assertThat(jwtTokenService.parseAccessToken(tokens.accessToken()).userId()).isEqualTo(7L);
        assertThat(jwtTokenService.parseRefreshToken(tokens.refreshToken()).userId()).isEqualTo(7L);
        assertThat(jwtTokenService.parseRefreshToken(tokens.refreshToken()).sessionKey()).isEqualTo(sessionKey);
        assertThatThrownBy(() -> jwtTokenService.parseAccessToken(tokens.refreshToken()))
                .isInstanceOfSatisfying(GeneralException.class,
                        ex -> assertThat(ex.getErrorCode().getCode()).isEqualTo("AUTH_401_001"));
    }

    @Test
    @DisplayName("OAuth state에는 제공자와 프론트 리다이렉트 URI가 위변조 방지 서명과 함께 저장된다")
    void issueAndParseOAuthState() {
        String state = jwtTokenService.issueOAuthState(
                OAuthProvider.KAKAO,
                "http://localhost:5173/oauth/callback"
        );

        var claims = jwtTokenService.parseOAuthState(state);

        assertThat(claims.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(claims.redirectUri()).isEqualTo("http://localhost:5173/oauth/callback");
        assertThatThrownBy(() -> jwtTokenService.parseRefreshToken(state))
                .isInstanceOfSatisfying(GeneralException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }
}
