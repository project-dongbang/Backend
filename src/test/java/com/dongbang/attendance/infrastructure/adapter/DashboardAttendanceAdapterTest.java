package com.dongbang.attendance.infrastructure.adapter;

import com.dongbang.attendance.infrastructure.persistence.AttendanceRecordJpaRepository;
import com.dongbang.attendance.infrastructure.persistence.DashboardAttendanceTotals;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.organization.application.facade.MembershipSummary;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DashboardAttendanceAdapterTest {
    private final AttendanceRecordJpaRepository records = mock(AttendanceRecordJpaRepository.class);
    private final MembershipAccessFacade memberships = mock(MembershipAccessFacade.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneOffset.UTC);
    private final DashboardAttendanceAdapter adapter = new DashboardAttendanceAdapter(records, memberships, clock);

    @Test
    void memberRateAndCountUseOneCompletedSessionAggregate() {
        MembershipSummary member = new MembershipSummary(7L, 1L, 2L, "회원", null, null);
        given(memberships.getMembershipSummary(1L, 2L)).willReturn(Optional.of(member));
        Instant cutoff = Instant.parse("2026-10-07T23:50:00Z");
        given(records.dashboardMemberTotals(1L, 7L, cutoff))
                .willReturn(new DashboardAttendanceTotals(3, 2));

        var stats = adapter.getMemberAttendanceStats(1L, 2L);

        assertThat(stats.attendanceRate()).isEqualTo(66.7);
        assertThat(stats.attendedEventCount()).isEqualTo(2);
        verify(records).dashboardMemberTotals(1L, 7L, cutoff);
    }
}
