package com.dongbang.dashboard.application.port;

import java.time.Instant;

public record DashboardScheduleItem(
        Long eventId,
        String day,
        Integer month,
        String title,
        String detail,
        String status,
        Instant startsAt
) {
}
