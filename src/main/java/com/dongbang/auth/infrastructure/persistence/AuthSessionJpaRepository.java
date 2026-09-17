package com.dongbang.auth.infrastructure.persistence;

import com.dongbang.auth.domain.AuthSession;
import com.dongbang.auth.domain.repository.AuthSessionRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthSessionJpaRepository extends JpaRepository<AuthSession, Long>, AuthSessionRepository {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AuthSession s WHERE s.sessionKey = :sessionKey")
    Optional<AuthSession> findBySessionKeyForUpdate(@Param("sessionKey") UUID sessionKey);
}
