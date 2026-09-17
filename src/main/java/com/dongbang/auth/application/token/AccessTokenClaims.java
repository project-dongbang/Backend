package com.dongbang.auth.application.token;

import java.time.Instant;

public record AccessTokenClaims(Long userId, Instant expiresAt) {
}
