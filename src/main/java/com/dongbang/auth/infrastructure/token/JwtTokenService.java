package com.dongbang.auth.infrastructure.token;

import com.dongbang.auth.application.token.AccessTokenClaims;
import com.dongbang.auth.application.token.OAuthStateClaims;
import com.dongbang.auth.application.token.RefreshTokenClaims;
import com.dongbang.auth.application.token.TokenPair;
import com.dongbang.auth.domain.OAuthProvider;
import com.dongbang.auth.exception.AuthErrorCode;
import com.dongbang.auth.infrastructure.config.AuthProperties;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.BaseErrorCode;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class JwtTokenService {

    private static final String TOKEN_TYPE = "token_type";
    private static final String ACCESS = "ACCESS";
    private static final String REFRESH = "REFRESH";
    private static final String OAUTH_STATE = "OAUTH_STATE";

    private final AuthProperties properties;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;

    @Autowired
    public JwtTokenService(AuthProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtTokenService(AuthProperties properties, Clock clock) {
        byte[] secretBytes = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET은 최소 32바이트여야 합니다.");
        }
        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        this.properties = properties;
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        this.decoder = jwtDecoder;
        this.clock = clock;
    }

    public TokenPair issueTokenPair(Long userId, UUID sessionKey) {
        Instant now = clock.instant();
        Instant accessExpiresAt = now.plus(properties.accessTokenTtl());
        Instant refreshExpiresAt = now.plus(properties.refreshTokenTtl());
        String accessToken = encode(userId, ACCESS, now, accessExpiresAt, null, null, null);
        String refreshToken = encode(userId, REFRESH, now, refreshExpiresAt, sessionKey, null, null);
        return new TokenPair(accessToken, accessExpiresAt, refreshToken, refreshExpiresAt, sessionKey);
    }

    public String issueOAuthState(OAuthProvider provider, String redirectUri) {
        Instant now = clock.instant();
        return encode(null, OAUTH_STATE, now, now.plus(properties.oauthStateTtl()), null, provider, redirectUri);
    }

    public AccessTokenClaims parseAccessToken(String token) {
        Jwt jwt = decode(token, ACCESS, GeneralErrorCode.UNAUTHORIZED);
        return new AccessTokenClaims(parseUserId(jwt), jwt.getExpiresAt());
    }

    public RefreshTokenClaims parseRefreshToken(String token) {
        Jwt jwt = decode(token, REFRESH, AuthErrorCode.INVALID_REFRESH_TOKEN);
        try {
            return new RefreshTokenClaims(
                    parseUserId(jwt),
                    UUID.fromString(jwt.getClaimAsString("session_key")),
                    jwt.getExpiresAt()
            );
        } catch (RuntimeException ex) {
            throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    public OAuthStateClaims parseOAuthState(String token) {
        Jwt jwt = decode(token, OAUTH_STATE, AuthErrorCode.INVALID_OAUTH_REQUEST);
        try {
            return new OAuthStateClaims(
                    OAuthProvider.valueOf(jwt.getClaimAsString("provider")),
                    jwt.getClaimAsString("redirect_uri")
            );
        } catch (RuntimeException ex) {
            throw new GeneralException(AuthErrorCode.INVALID_OAUTH_REQUEST);
        }
    }

    public long accessTokenExpiresInSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    private String encode(Long userId, String type, Instant issuedAt, Instant expiresAt, UUID sessionKey,
                          OAuthProvider provider, String redirectUri) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE, type);
        if (userId != null) {
            claims.subject(userId.toString());
        } else {
            claims.subject("oauth-state");
        }
        if (sessionKey != null) {
            claims.claim("session_key", sessionKey.toString());
        }
        if (provider != null) {
            claims.claim("provider", provider.name());
        }
        if (redirectUri != null) {
            claims.claim("redirect_uri", redirectUri);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    private Jwt decode(String token, String expectedType, BaseErrorCode errorCode) {
        if (token == null || token.isBlank()) {
            throw new GeneralException(errorCode);
        }
        try {
            Jwt jwt = decoder.decode(token);
            if (!expectedType.equals(jwt.getClaimAsString(TOKEN_TYPE))) {
                throw new GeneralException(errorCode);
            }
            return jwt;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new GeneralException(errorCode);
        }
    }

    private Long parseUserId(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (RuntimeException ex) {
            throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
}
