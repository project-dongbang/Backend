package com.dongbang.event.application.result;

import com.dongbang.event.domain.EventType;
import java.time.Instant;

public record EventDetailResult(
        Long eventId,
        Long organizationId,
        EventType type,
        String title,
        Instant startsAt,
        Instant endsAt,
        String location,
        String description,
        Integer capacity,
        Instant registrationDeadline,
        String registrationStatus,
        Integer participantCount,
        Boolean participating,
        Boolean canApply,
        Boolean canCancel,
        Long participantVersion,
        String attendanceSessionStatus
) {
}
