package com.dongbang.dashboard.presentation.dto;

public record MemberPersonalStatsResponse(
        double attendanceRate,
        int attendedEventCount,
        int unpaidFeeCount
) {
}
