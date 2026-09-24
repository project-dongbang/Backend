package com.dongbang.attendance.domain;

import com.dongbang.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "attendance_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceRecord extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_record_id") private Long id;
    @Column(name = "attendance_session_id", nullable = false) private Long attendanceSessionId;
    @Column(name = "membership_id", nullable = false) private Long membershipId;
    @Column(name = "recorded_by_membership_id") private Long recordedByMembershipId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private AttendanceStatus status;
    @Enumerated(EnumType.STRING)
    @Column(length = 20) private AttendanceSource source;
    @Column(length = 500) private String reason;
    @Column(name = "checked_at") private Instant checkedAt;
    @Version @Column(nullable = false) private long version;

    public AttendanceRecord(Long sessionId, Long membershipId) {
        this.attendanceSessionId = sessionId;
        this.membershipId = membershipId;
        this.status = AttendanceStatus.ABSENT;
    }

    public void checkIn(Instant now) {
        status = AttendanceStatus.PRESENT;
        source = AttendanceSource.QR;
        checkedAt = now;
        recordedByMembershipId = membershipId;
        reason = null;
    }

    public void modify(AttendanceStatus next, Long actorId, String reason, Instant now) {
        status = next;
        source = AttendanceSource.MANUAL;
        checkedAt = next == AttendanceStatus.PRESENT ? now : null;
        recordedByMembershipId = actorId;
        this.reason = reason;
    }
}
