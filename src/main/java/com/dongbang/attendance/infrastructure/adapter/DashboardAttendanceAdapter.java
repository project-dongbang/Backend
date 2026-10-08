package com.dongbang.attendance.infrastructure.adapter;

import com.dongbang.attendance.domain.AttendanceSession;
import com.dongbang.attendance.infrastructure.persistence.AttendanceRecordJpaRepository;
import com.dongbang.attendance.infrastructure.persistence.DashboardAttendanceTotals;
import com.dongbang.dashboard.application.port.DashboardAttendancePort;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.organization.application.facade.MembershipSummary;
import com.dongbang.organization.exception.OrganizationErrorCode;
import com.dongbang.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class DashboardAttendanceAdapter implements DashboardAttendancePort {
    private final AttendanceRecordJpaRepository records;
    private final MembershipAccessFacade memberships;
    private final Clock clock;

    @Override
    public double getOrganizationAttendanceRate(Long organizationId) {
        return records.dashboardTotals(organizationId, expiredBefore()).rate();
    }

    @Override
    public MemberAttendanceStats getMemberAttendanceStats(Long organizationId, Long userId) {
        DashboardAttendanceTotals totals = memberTotals(organizationId, userId);
        return new MemberAttendanceStats(totals.rate(), Math.toIntExact(totals.present()));
    }

    private DashboardAttendanceTotals memberTotals(Long organizationId, Long userId) {
        Long membershipId = memberships.getMembershipSummary(organizationId, userId)
                .map(MembershipSummary::membershipId)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.MEMBER_REQUIRED));
        return records.dashboardMemberTotals(organizationId, membershipId, expiredBefore());
    }

    private java.time.Instant expiredBefore() {
        return clock.instant().minusSeconds(AttendanceSession.QR_TTL_SECONDS);
    }
}
