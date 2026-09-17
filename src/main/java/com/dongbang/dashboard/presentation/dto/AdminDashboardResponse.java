package com.dongbang.dashboard.presentation.dto;

import com.dongbang.dashboard.application.port.DashboardScheduleItem;

import java.util.List;

public record AdminDashboardResponse(
        Long organizationId,
        String organizationName,
        DashboardStatsResponse stats,
        List<DashboardScheduleItem> upcomingSchedules,
        List<DashboardPhotoItem> recentPhotos
) {
}
