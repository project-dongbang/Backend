package com.dongbang.event.application.result;

import java.time.Instant;
import java.time.LocalDate;

public record CalendarEventResult(
        Long eventId,
        Long feeItemId,
        CalendarItemType type,
        String title,
        Instant startsAt,
        Instant endsAt,
        LocalDate dueDate
) {
}
