package com.dongbang.event.infrastructure.adapter;

import com.dongbang.event.domain.Event;
import com.dongbang.event.domain.EventDetails;
import com.dongbang.event.domain.EventParticipant;
import com.dongbang.event.domain.EventType;
import com.dongbang.event.domain.repository.EventParticipantRepository;
import com.dongbang.event.domain.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MyPageEventActivityAdapterTest {
    @Mock EventParticipantRepository participantRepository;
    @Mock EventRepository eventRepository;

    @Test
    void excludesSchedulesAndReturnsEventsInStartOrder() {
        var laterRegistration = new EventParticipant(32L, 20L, Instant.parse("2026-09-01T00:00:00Z"));
        var earlierRegistration = new EventParticipant(31L, 20L, Instant.parse("2026-09-02T00:00:00Z"));
        var scheduleRegistration = new EventParticipant(33L, 20L, Instant.parse("2026-09-03T00:00:00Z"));
        given(participantRepository.findByMembershipId(20L))
                .willReturn(List.of(laterRegistration, earlierRegistration, scheduleRegistration));
        given(eventRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(32L, 10L))
                .willReturn(Optional.of(event(32L, EventType.EVENT, "나중 행사", "2026-09-20T10:00:00Z")));
        given(eventRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(31L, 10L))
                .willReturn(Optional.of(event(31L, EventType.EVENT, "먼저 행사", "2026-09-17T10:00:00Z")));
        given(eventRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(33L, 10L))
                .willReturn(Optional.of(event(33L, EventType.SCHEDULE, "일정", "2026-09-16T10:00:00Z")));

        var result = new MyPageEventActivityAdapter(participantRepository, eventRepository)
                .findRegisteredEvents(10L, 20L);

        assertThat(result).extracting(item -> item.eventId()).containsExactly(31L, 32L);
    }

    private Event event(Long id, EventType type, String title, String startsAt) {
        Instant starts = Instant.parse(startsAt);
        return Event.builder().id(id).organizationId(10L).createdByMembershipId(9L).type(type)
                .details(new EventDetails(title, null, "동아리방", starts, starts.plusSeconds(3600), null, null))
                .build();
    }
}
