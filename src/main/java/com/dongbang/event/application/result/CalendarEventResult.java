package com.dongbang.event.application.result;

import java.time.Instant;
import java.time.LocalDate;

public record CalendarEventResult(
        Long eventId,
        Long feeItemId,
        CalendarItemType type,
        com.dongbang.event.domain.EventStatus status,
        String title,
        Instant startsAt,
        Instant endsAt,
        String location,
        String registrationStatus,
        Integer capacity,
        Integer participantCount,
        Boolean participating,
        LocalDate dueDate
) {
}
