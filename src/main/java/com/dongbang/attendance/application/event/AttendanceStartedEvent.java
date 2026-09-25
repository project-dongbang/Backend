package com.dongbang.attendance.application.event;

import java.time.Instant;
import java.util.List;

public record AttendanceStartedEvent(
        Long organizationId,
        Long eventId,
        Long attendanceSessionId,
        String eventTitle,
        Instant startedAt,
        List<Long> participantMembershipIds
) {
    public AttendanceStartedEvent {
        participantMembershipIds = List.copyOf(participantMembershipIds);
    }
}
