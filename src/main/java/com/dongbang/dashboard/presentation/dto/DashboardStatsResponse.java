package com.dongbang.dashboard.presentation.dto;

public record DashboardStatsResponse(
        long activeMemberCount,
        int thisMonthEventCount,
        double attendanceRate,
        double paymentRate
) {
}
