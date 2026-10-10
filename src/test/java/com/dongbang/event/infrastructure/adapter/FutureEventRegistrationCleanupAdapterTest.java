package com.dongbang.event.infrastructure.adapter;

import com.dongbang.event.application.port.EventActivityPort;
import com.dongbang.event.domain.Event;
import com.dongbang.event.domain.EventDetails;
import com.dongbang.event.domain.EventParticipant;
import com.dongbang.event.domain.EventType;
import com.dongbang.event.domain.repository.EventParticipantRepository;
import com.dongbang.event.domain.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FutureEventRegistrationCleanupAdapterTest {
    @Mock EventParticipantRepository participants;
    @Mock EventRepository events;
    @Mock EventActivityPort activity;
    @InjectMocks FutureEventRegistrationCleanupAdapter adapter;

    private final Instant now = Instant.parse("2026-10-11T00:00:00Z");

    @Test
    void cancelsFutureRegistrationAndFreesSeat() {
        var registration = new EventParticipant(10L, 20L, now.minusSeconds(60));
        var event = event(10L, now.plusSeconds(3600));
        given(participants.findByMembershipId(20L)).willReturn(List.of(registration));
        given(events.findForUpdate(10L, 1L)).willReturn(Optional.of(event));

        adapter.cancelFutureRegistrations(20L, 1L, now);

        assertThat(registration.getStatus()).isEqualTo("CANCELED");
        assertThat(registration.getCanceledAt()).isEqualTo(now);
        assertThat(event.getParticipantVersion()).isEqualTo(1);
        verify(activity).synchronizeParticipants(10L, List.of(), List.of(20L));
    }

    @Test
    void preservesPastRegistration() {
        var registration = new EventParticipant(10L, 20L, now.minusSeconds(7200));
        var event = event(10L, now.minusSeconds(3600));
        given(participants.findByMembershipId(20L)).willReturn(List.of(registration));
        given(events.findForUpdate(10L, 1L)).willReturn(Optional.of(event));

        adapter.cancelFutureRegistrations(20L, 1L, now);

        assertThat(registration.getStatus()).isEqualTo("REGISTERED");
        assertThat(event.getParticipantVersion()).isZero();
        verifyNoInteractions(activity);
    }

    private Event event(Long id, Instant startsAt) {
        return Event.builder().id(id).organizationId(1L).createdByMembershipId(2L)
                .type(EventType.EVENT)
                .details(new EventDetails("행사", null, "장소", startsAt,
                        startsAt.plusSeconds(3600), null, null)).build();
    }
}
