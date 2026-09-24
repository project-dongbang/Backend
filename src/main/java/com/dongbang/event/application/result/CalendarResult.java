package com.dongbang.event.application.result;

import java.util.List;

public record CalendarResult(int year, int month, List<CalendarEventResult> events) {
}
