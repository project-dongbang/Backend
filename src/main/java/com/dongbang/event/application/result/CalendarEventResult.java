package com.dongbang.event.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
        LocalDate dueDate,
        String description,
        BigDecimal memberAmount,
        List<BigDecimal> amountOptions,
        Long targetCount,
        Long paidCount,
        Long unpaidCount
) {
}
