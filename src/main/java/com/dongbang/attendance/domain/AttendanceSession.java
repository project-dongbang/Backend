package com.dongbang.attendance.domain;

import com.dongbang.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "attendance_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceSession extends BaseTimeEntity {
    public static final int QR_TTL_SECONDS = 600;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_session_id") private Long id;
    @Column(name = "event_id", nullable = false, unique = true) private Long eventId;
    @Column(name = "opened_by_membership_id", nullable = false) private Long openedByMembershipId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private AttendanceSessionStatus status;
    @Column(name = "qr_token", nullable = false, unique = true, length = 100) private String qrToken;
    @Column(name = "qr_version", nullable = false) private int qrVersion;
    @Column(name = "qr_ttl_seconds", nullable = false) private int qrTtlSeconds;
    @Column(name = "opened_at", nullable = false) private Instant openedAt;
    @Column(name = "closed_at") private Instant closedAt;

    public AttendanceSession(Long eventId, Long openerId, String qrToken, Instant openedAt) {
        this.eventId = eventId;
        this.openedByMembershipId = openerId;
        this.status = AttendanceSessionStatus.OPEN;
        this.qrToken = qrToken;
        this.qrVersion = 1;
        this.qrTtlSeconds = QR_TTL_SECONDS;
        this.openedAt = openedAt;
    }

    public Instant expiresAt() {
        return openedAt.plusSeconds(qrTtlSeconds);
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt());
    }

    public void close(Instant now) {
        if (status == AttendanceSessionStatus.OPEN && !isExpired(now)) {
            status = AttendanceSessionStatus.CLOSED;
            closedAt = now;
        }
    }
}
