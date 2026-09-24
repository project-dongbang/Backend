package com.dongbang.event.application.facade;

import com.dongbang.event.domain.Event;
import com.dongbang.event.domain.EventParticipant;
import com.dongbang.event.domain.EventStatus;
import com.dongbang.event.domain.EventType;
import com.dongbang.event.domain.repository.EventParticipantRepository;
import com.dongbang.event.domain.repository.EventRepository;
import com.dongbang.event.exception.EventErrorCode;
import com.dongbang.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventAttendanceFacade {
    private final EventRepository events;
    private final EventParticipantRepository participants;

    public AttendanceEvent event(Long organizationId, Long eventId) {
        return toAttendanceEvent(requireEvent(events.findByIdAndOrganizationIdAndDeletedAtIsNull(eventId, organizationId)
                .orElseThrow(() -> new GeneralException(EventErrorCode.EVENT_NOT_FOUND))));
    }

    @Transactional
    public AttendanceEvent lockEvent(Long organizationId, Long eventId) {
        return toAttendanceEvent(requireEvent(events.findForUpdate(eventId, organizationId)
                .orElseThrow(() -> new GeneralException(EventErrorCode.EVENT_NOT_FOUND))));
    }

    public List<AttendanceEvent> events(Long organizationId) {
        return events.findAttendanceEvents(organizationId).stream().map(this::toAttendanceEvent).toList();
    }

    public List<Long> participantIds(Long eventId) {
        return participants.findByEventIdOrderByRegisteredAtAscIdAsc(eventId).stream()
                .map(EventParticipant::getMembershipId).toList();
    }

    public boolean isParticipant(Long eventId, Long membershipId) {
        return participants.findByEventIdAndMembershipId(eventId, membershipId).isPresent();
    }

    public List<AttendanceEvent> registeredEvents(Long organizationId, Long membershipId) {
        return participants.findByMembershipId(membershipId).stream()
                .map(p -> events.findByIdAndOrganizationIdAndDeletedAtIsNull(p.getEventId(), organizationId).orElse(null))
                .filter(e -> e != null && e.getType() == EventType.EVENT)
                .map(this::toAttendanceEvent)
                .toList();
    }

    private Event requireEvent(Event event) {
        if (event.getType() != EventType.EVENT) throw new GeneralException(EventErrorCode.EVENT_ONLY);
        if (event.getStatus() == EventStatus.CANCELED) throw new GeneralException(EventErrorCode.EVENT_CANCELED);
        return event;
    }

    private AttendanceEvent toAttendanceEvent(Event event) {
        return new AttendanceEvent(event.getId(), event.getOrganizationId(), event.getTitle(), event.getStartsAt(),
                event.getEndsAt(), event.getLocation());
    }

    public record AttendanceEvent(Long eventId, Long organizationId, String title,
                                  Instant startsAt, Instant endsAt, String location) {}
}
