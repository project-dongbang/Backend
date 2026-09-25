package com.dongbang.event.domain.repository;

import com.dongbang.event.domain.Event;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventRepository {
    Event save(Event event);
    Optional<Event> findByIdAndOrganizationIdAndDeletedAtIsNull(Long id, Long organizationId);
    Optional<Event> findForUpdate(Long id, Long organizationId);
    List<Event> findOverlapping(Long organizationId, Instant from, Instant until);
    List<Event> findAttendanceEvents(Long organizationId);
    List<Event> findScheduledEventsStartingBetween(Instant from, Instant until);
}
