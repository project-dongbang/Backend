package com.dongbang.auth.application.token;

import java.time.Instant;
import java.util.UUID;

public record TokenPair(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UUID sessionKey
) {
}
