package com.dongbang.event.application.result;

import com.dongbang.event.domain.EventType;
import java.time.Instant;

public record CalendarEventResult(
        Long eventId,
        EventType type,
        com.dongbang.event.domain.EventStatus status,
        String title,
        Instant startsAt,
        Instant endsAt,
        String location,
        String registrationStatus,
        Integer capacity,
        Integer participantCount,
        Boolean participating
) {
}
