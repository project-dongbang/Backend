package com.dongbang.dashboard.presentation.dto;

import com.dongbang.dashboard.application.port.DashboardScheduleItem;

import java.util.List;

public record MemberDashboardResponse(
        Long organizationId,
        String organizationName,
        MemberPersonalStatsResponse myStats,
        DashboardStatsResponse stats,
        List<DashboardScheduleItem> upcomingSchedules,
        List<DashboardPhotoItem> recentPhotos
) {
}
