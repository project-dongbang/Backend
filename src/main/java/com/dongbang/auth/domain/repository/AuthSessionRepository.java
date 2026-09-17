package com.dongbang.auth.domain.repository;

import com.dongbang.auth.domain.AuthSession;

import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository {
    AuthSession save(AuthSession session);
    Optional<AuthSession> findBySessionKeyForUpdate(UUID sessionKey);
}
