package com.dongbang.event.presentation.dto.response;

import com.dongbang.event.application.result.EventDetailResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.dongbang.event.domain.EventType;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventDetailResponse(
        Long eventId,
        Long organizationId,
        EventType type,
        String title,
        Instant startsAt,
        Instant endsAt,
        String location,
        String description,
        @JsonIgnore Integer capacity,
        Instant registrationDeadline,
        String registrationStatus,
        Integer participantCount,
        Boolean participating,
        Boolean canApply,
        Boolean canCancel,
        Long participantVersion,
        String attendanceSessionStatus
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
    public static EventDetailResponse from(EventDetailResult result) {
        return new EventDetailResponse(result.eventId(), result.organizationId(), result.type(), result.title(), result.startsAt(), result.endsAt(), result.location(), result.description(), result.capacity(), result.registrationDeadline(), result.registrationStatus(), result.participantCount(), result.participating(), result.canApply(), result.canCancel(), result.participantVersion(), result.attendanceSessionStatus());
    }
}
