package com.dongbang.event.presentation.dto.response;

import com.dongbang.event.application.result.CalendarEventResult;
import com.dongbang.event.application.result.CalendarItemType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CalendarEventResponse(
        Long eventId,
        Long feeItemId,
        CalendarItemType type,
        String title,
        Instant startsAt,
        Instant endsAt,
        LocalDate dueDate
) {
    public static CalendarEventResponse from(CalendarEventResult result) {
        return new CalendarEventResponse(result.eventId(), result.feeItemId(), result.type(), result.title(),
                result.startsAt(), result.endsAt(), result.dueDate());
    }
}
