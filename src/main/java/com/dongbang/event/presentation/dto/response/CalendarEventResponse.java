package com.dongbang.event.presentation.dto.response;

import com.dongbang.event.application.result.CalendarEventResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.dongbang.event.domain.EventType;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CalendarEventResponse(
        Long eventId,
        EventType type,
        String title,
        Instant startsAt,
        Instant endsAt,
        String location,
        String registrationStatus,
        @JsonIgnore Integer capacity,
        Integer participantCount,
        Boolean participating
) {
    // 정원 제한 없는 행사: capacity:null 반환 / 일반 일정: 필드 생략
    @JsonAnyGetter
    @JsonInclude(content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> eventCapacity() {
        java.util.Map<String, Object> fields = new java.util.LinkedHashMap<>();
        if (type == EventType.EVENT) {
            fields.put("capacity", capacity);
        }
        return fields;
    }
    public static CalendarEventResponse from(CalendarEventResult result) {
        return new CalendarEventResponse(result.eventId(), result.type(), result.title(), result.startsAt(), result.endsAt(), result.location(), result.registrationStatus(), result.capacity(), result.participantCount(), result.participating());
    }
}
