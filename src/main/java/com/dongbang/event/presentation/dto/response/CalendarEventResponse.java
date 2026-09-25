package com.dongbang.event.presentation.dto.response;

import com.dongbang.event.application.result.CalendarEventResult;
import com.dongbang.event.application.result.CalendarItemType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAnyGetter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CalendarEventResponse(
        Long eventId,
        Long feeItemId,
        CalendarItemType type,
        com.dongbang.event.domain.EventStatus status,
        String title,
        Instant startsAt,
        Instant endsAt,
        String location,
        String registrationStatus,
        @JsonIgnore Integer capacity,
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
    // 정원 제한 없는 행사: capacity:null 반환 / 일반 일정: 필드 생략
    @JsonAnyGetter
    @JsonInclude(content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> eventCapacity() {
        java.util.Map<String, Object> fields = new java.util.LinkedHashMap<>();
        if (type == CalendarItemType.EVENT) {
            fields.put("capacity", capacity);
        }
        return fields;
    }
    public static CalendarEventResponse from(CalendarEventResult result) {
        return new CalendarEventResponse(result.eventId(), result.feeItemId(), result.type(), result.status(),
                result.title(), result.startsAt(), result.endsAt(), result.location(), result.registrationStatus(),
                result.capacity(), result.participantCount(), result.participating(), result.dueDate(),
                result.description(), result.memberAmount(), result.amountOptions(), result.targetCount(),
                result.paidCount(), result.unpaidCount());
    }
}
