package com.dongbang.dashboard.application;

import com.dongbang.dashboard.application.port.DashboardAttendancePort;
import com.dongbang.dashboard.application.port.DashboardEventPort;
import com.dongbang.dashboard.application.port.DashboardFinancePort;
import com.dongbang.dashboard.application.port.DashboardScheduleItem;
import com.dongbang.dashboard.exception.DashboardErrorCode;
import com.dongbang.dashboard.presentation.dto.*;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.organization.domain.Organization;
import com.dongbang.organization.domain.OrganizationStatus;
import com.dongbang.organization.domain.repository.OrganizationRepository;
import com.dongbang.photo.application.facade.PhotoAccessFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardQueryService {

    private final OrganizationRepository organizationRepository;
    private final MembershipAccessFacade membershipAccessFacade;
    private final PhotoAccessFacade photoAccessFacade;
    private final DashboardEventPort eventPort;
    private final DashboardAttendancePort attendancePort;
    private final DashboardFinancePort financePort;

    public AdminDashboardResponse getAdminDashboard(Long organizationId, Long userId) {
        if (!membershipAccessFacade.isStaff(organizationId, userId)) {
            throw new GeneralException(DashboardErrorCode.STAFF_REQUIRED);
        }

        Organization org = findActiveOrganization(organizationId);
        DashboardStatsResponse stats = buildOverviewStats(organizationId);
        List<DashboardScheduleItem> upcomingSchedules = eventPort.getUpcomingSchedules(organizationId, 5);
        List<DashboardPhotoItem> recentPhotos = fetchRecentPhotos(organizationId);

        return new AdminDashboardResponse(
                org.getId(),
                org.getName(),
                stats,
                upcomingSchedules,
                recentPhotos
        );
    }

    public MemberDashboardResponse getMemberDashboard(Long organizationId, Long userId) {
        if (!membershipAccessFacade.isActiveMember(organizationId, userId)) {
            throw new GeneralException(DashboardErrorCode.MEMBER_REQUIRED);
        }

        Organization org = findActiveOrganization(organizationId);

        double myAttendanceRate = attendancePort.getMemberAttendanceRate(organizationId, userId);
        int attendedEventCount = attendancePort.getMemberAttendanceCount(organizationId, userId);
        int unpaidFeeCount = financePort.getMemberUnpaidFeeCount(organizationId, userId);

        MemberPersonalStatsResponse myStats = new MemberPersonalStatsResponse(
                myAttendanceRate,
                attendedEventCount,
                unpaidFeeCount
        );

        DashboardStatsResponse stats = buildOverviewStats(organizationId);
        List<DashboardScheduleItem> upcomingSchedules = eventPort.getUpcomingSchedules(organizationId, 5);
        List<DashboardPhotoItem> recentPhotos = fetchRecentPhotos(organizationId);

        return new MemberDashboardResponse(
                org.getId(),
                org.getName(),
                myStats,
                stats,
                upcomingSchedules,
                recentPhotos
        );
    }

    private Organization findActiveOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .filter(o -> o.getStatus() == OrganizationStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(DashboardErrorCode.ORGANIZATION_NOT_FOUND));
    }

    private DashboardStatsResponse buildOverviewStats(Long organizationId) {
        long activeMemberCount = membershipAccessFacade.getActiveMemberCount(organizationId);
        int thisMonthEventCount = eventPort.countThisMonthEvents(organizationId);
        double attendanceRate = attendancePort.getOrganizationAttendanceRate(organizationId);
        double paymentRate = financePort.getFeePaymentRate(organizationId);

        return new DashboardStatsResponse(
                activeMemberCount,
                thisMonthEventCount,
                attendanceRate,
                paymentRate
        );
    }

    private List<DashboardPhotoItem> fetchRecentPhotos(Long organizationId) {
        return photoAccessFacade.getRecentPhotos(organizationId, 4).stream()
                .map(p -> new DashboardPhotoItem(p.photoId(), p.title(), p.imageUrl()))
                .toList();
    }
}
