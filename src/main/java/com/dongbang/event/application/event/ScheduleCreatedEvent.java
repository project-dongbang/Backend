package com.dongbang.event.application.event;

import java.time.Instant;

public record ScheduleCreatedEvent(
        Long organizationId,
        Long eventId,
        String title,
        Instant startsAt
) {
}
