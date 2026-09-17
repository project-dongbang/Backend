package com.dongbang.auth.domain;

import com.dongbang.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_session_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_key", nullable = false, unique = true)
    private UUID sessionKey;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
    private String refreshTokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    private AuthSession(Long userId, UUID sessionKey, String refreshTokenHash, Instant expiresAt,
                        String deviceInfo, String ipAddress) {
        this.userId = userId;
        this.sessionKey = sessionKey;
        this.refreshTokenHash = refreshTokenHash;
        this.expiresAt = expiresAt;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
    }

    public static AuthSession create(Long userId, UUID sessionKey, String refreshTokenHash, Instant expiresAt,
                                     String deviceInfo, String ipAddress) {
        return new AuthSession(userId, sessionKey, refreshTokenHash, expiresAt, deviceInfo, ipAddress);
    }

    public boolean isActiveAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void rotate(String newRefreshTokenHash, Instant newExpiresAt, Instant usedAt,
                       String deviceInfo, String ipAddress) {
        this.refreshTokenHash = newRefreshTokenHash;
        this.expiresAt = newExpiresAt;
        this.lastUsedAt = usedAt;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
    }

    public void revoke(Instant revokedAt) {
        if (this.revokedAt == null) {
            this.revokedAt = revokedAt;
        }
    }
}
