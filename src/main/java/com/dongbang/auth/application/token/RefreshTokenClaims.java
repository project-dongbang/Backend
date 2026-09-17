package com.dongbang.auth.application.token;

import java.time.Instant;
import java.util.UUID;

public record RefreshTokenClaims(Long userId, UUID sessionKey, Instant expiresAt) {
}
