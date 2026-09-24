package com.dongbang.event.presentation.dto.response;

import com.dongbang.event.application.result.CalendarResult;
import java.util.List;

public record CalendarResponse(int year, int month, List<CalendarEventResponse> events) {
    public static CalendarResponse from(CalendarResult result) {
        return new CalendarResponse(result.year(), result.month(), result.events().stream().map(CalendarEventResponse::from).toList());
    }
}
