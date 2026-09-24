package com.dongbang.attendance.application;

import com.dongbang.attendance.domain.AttendanceRecord;
import com.dongbang.attendance.domain.AttendanceSession;
import com.dongbang.attendance.domain.AttendanceSessionStatus;
import com.dongbang.attendance.domain.AttendanceSource;
import com.dongbang.attendance.domain.AttendanceStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AttendancePolicyTest {
    private final Instant startedAt = Instant.parse("2026-09-24T09:00:00Z");

    @Test
    void qrExpiresExactlyTenMinutesAfterStart() {
        AttendanceSession session = new AttendanceSession(1L, 2L, "token", startedAt);

        assertThat(session.expiresAt()).isEqualTo(startedAt.plusSeconds(600));
        assertThat(session.isExpired(startedAt.plusSeconds(599))).isFalse();
        assertThat(session.isExpired(startedAt.plusSeconds(600))).isTrue();
    }

    @Test
    void expiredSessionIsNotChangedToClosed() {
        AttendanceSession session = new AttendanceSession(1L, 2L, "token", startedAt);

        session.close(startedAt.plusSeconds(600));

        assertThat(session.getStatus()).isEqualTo(AttendanceSessionStatus.OPEN);
        assertThat(session.getClosedAt()).isNull();
    }

    @Test
    void manualAbsenceClearsPreviousCheckIn() {
        AttendanceRecord record = new AttendanceRecord(1L, 3L);
        record.checkIn(startedAt);

        record.modify(AttendanceStatus.ABSENT, 2L, "오등록 수정", startedAt.plusSeconds(700));

        assertThat(record.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(record.getSource()).isEqualTo(AttendanceSource.MANUAL);
        assertThat(record.getCheckedAt()).isNull();
        assertThat(record.getReason()).isEqualTo("오등록 수정");
    }
}
