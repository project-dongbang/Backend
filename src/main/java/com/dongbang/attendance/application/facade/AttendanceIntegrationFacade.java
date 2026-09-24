package com.dongbang.attendance.application.facade;

import com.dongbang.attendance.application.dto.AttendanceResults.SessionViewStatus;
import com.dongbang.attendance.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AttendanceIntegrationFacade {
    private final AttendanceSessionRepository sessions;
    private final AttendanceRecordRepository records;
    private final Clock clock;

    @Transactional(readOnly = true)
    public SessionViewStatus status(Long eventId) {
        return sessions.findByEventId(eventId).map(session -> {
            if (session.getStatus() == AttendanceSessionStatus.CLOSED) return SessionViewStatus.CLOSED;
            return session.isExpired(clock.instant()) ? SessionViewStatus.EXPIRED : SessionViewStatus.ACTIVE;
        }).orElse(SessionViewStatus.NOT_STARTED);
    }

    @Transactional
    public void synchronizeParticipants(Long eventId, List<Long> added, List<Long> removed) {
        AttendanceSession session = sessions.findByEventId(eventId).orElse(null);
        if (session == null) return;
        for (Long membershipId : added) {
            AttendanceRecord record = records.findByAttendanceSessionIdAndMembershipId(session.getId(), membershipId)
                    .orElseGet(() -> records.save(new AttendanceRecord(session.getId(), membershipId)));
            record.activate();
        }
        for (Long membershipId : removed) {
            records.findByAttendanceSessionIdAndMembershipId(session.getId(), membershipId)
                    .ifPresent(AttendanceRecord::deactivate);
        }
    }
}
