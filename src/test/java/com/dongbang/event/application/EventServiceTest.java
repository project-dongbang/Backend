package com.dongbang.event.application;

import com.dongbang.event.application.port.EventActivity;
import com.dongbang.event.application.port.EventActivityPort;
import com.dongbang.event.domain.*;
import com.dongbang.event.domain.repository.EventRepository;
import com.dongbang.event.exception.EventErrorCode;
import com.dongbang.event.application.command.CreateEventCommand;
import com.dongbang.event.application.command.UpdateEventCommand;
import java.util.Set;
import com.dongbang.global.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository repository;
    @Mock EventAccessService access;
    @Mock EventActivityPort activityPort;
    @Mock ApplicationEventPublisher eventPublisher;

    private final Instant now = Instant.parse("2026-09-01T00:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private EventCommandService commands;
    private EventQueryService queries;
    private Event event;

    @BeforeEach
    void setUp() {
        commands = new EventCommandService(repository, access, activityPort, clock, eventPublisher);
        queries = new EventQueryService(repository, access, activityPort, clock);
        event = Event.builder().id(101L).organizationId(1L).createdByMembershipId(9L)
                .type(EventType.EVENT)
                .details(new EventDetails("행사", "설명", "동아리방",
                        now.plusSeconds(3600), now.plusSeconds(7200), 20, null))
                .build();
    }

    @Test
    void calendarUsesKoreanMonthBoundaries() {
        Instant from = Instant.parse("2026-08-31T15:00:00Z");
        Instant until = Instant.parse("2026-09-30T15:00:00Z");
        when(repository.findOverlapping(1L, from, until)).thenReturn(List.of());
        assertThat(queries.calendar(1L, 1L, 2026, 9).events()).isEmpty();
        verify(access).requireMember(1L, 1L);
        verify(repository).findOverlapping(1L, from, until);
    }

    @Test
    void missingEventIsScopedToOrganization() {
        when(repository.findByIdAndOrganizationIdAndDeletedAtIsNull(101L, 2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> queries.detail(2L, 1L, 101L))
                .isInstanceOfSatisfying(GeneralException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(EventErrorCode.EVENT_NOT_FOUND));
    }

    @Test
    void creationUsesMembershipNotUserId() {
        when(access.requireStaff(1L, 77L)).thenReturn(9L);
        when(repository.save(any())).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            assertThat(saved.getCreatedByMembershipId()).isEqualTo(9L);
            assertThat(saved.getRegistrationDeadline()).isEqualTo(saved.getStartsAt());
            return saved;
        });
        commands.create(1L, 77L, new CreateEventCommand(EventType.EVENT,
                new EventDetails("행사", null, "장소", now.plusSeconds(1), now.plusSeconds(2), null, null)));
        verify(repository).save(any());
    }

    @Test
    void scheduleCreationPublishesNotificationEvent() {
        when(access.requireStaff(1L, 77L)).thenReturn(9L);
        when(repository.save(any())).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 101L);
            return saved;
        });

        commands.create(1L, 77L, new CreateEventCommand(EventType.SCHEDULE,
                new EventDetails("운영진 회의", null, "동아리방",
                        now.plusSeconds(3600), now.plusSeconds(7200), null, null)));

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue())
                .isInstanceOfSatisfying(com.dongbang.event.application.event.ScheduleCreatedEvent.class, created -> {
                    assertThat(created.organizationId()).isEqualTo(1L);
                    assertThat(created.eventId()).isEqualTo(101L);
                    assertThat(created.title()).isEqualTo("운영진 회의");
                });
    }

    @Test
    void rejectsReversedTime() {
        assertThatThrownBy(() -> event.revise(new EventDetails("행사", null, "장소", now, now, 20, null)))
                .isInstanceOfSatisfying(GeneralException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(EventErrorCode.INVALID_EVENT_TIME));
    }

    @Test
    void scheduleCannotHaveRegistrationFields() {
        assertThatThrownBy(() -> Event.builder().type(EventType.SCHEDULE)
                .details(new EventDetails("일정", null, "장소", now, now.plusSeconds(1), 1, null)).build())
                .isInstanceOf(GeneralException.class);
    }

    @Test
    void partialPatchKeepsOmittedFieldsAndClearsExplicitNull() {
        prepareUpdate(new EventActivity(0, false, 0L, "NOT_STARTED", false));
        UpdateEventCommand patch = new UpdateEventCommand(Set.of("description", "capacity"),
                null, null, null, null, null, null, null);
        commands.update(1L, 1L, 101L, patch);
        assertThat(event.getDescription()).isNull();
        assertThat(event.getCapacity()).isNull();
        assertThat(event.getTitle()).isEqualTo("행사");
    }

    @Test
    void rejectsCapacityBelowParticipantCount() {
        prepareUpdate(new EventActivity(12, false, 0L, "NOT_STARTED", false));
        UpdateEventCommand patch = new UpdateEventCommand(Set.of("capacity"),
                null, null, null, null, null, 11, null);
        assertThatThrownBy(() -> commands.update(1L, 1L, 101L, patch))
                .isInstanceOfSatisfying(GeneralException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(EventErrorCode.CAPACITY_EXCEEDED));
        assertThat(event.getCapacity()).isEqualTo(20);
    }

    @Test
    void rejectsTimeChangeAfterAttendanceStart() {
        prepareUpdate(new EventActivity(0, false, 0L, "EXPIRED", false));
        UpdateEventCommand patch = new UpdateEventCommand(Set.of("endsAt"),
                null, null, null, null, now.plusSeconds(8000), null, null);
        assertThatThrownBy(() -> commands.update(1L, 1L, 101L, patch))
                .isInstanceOfSatisfying(GeneralException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(EventErrorCode.ATTENDANCE_ALREADY_STARTED));
    }

    @Test
    void deletingAfterAttendanceStartIsRejected() {
        prepareUpdate(new EventActivity(0, false, 0L, "CLOSED", false));
        assertThatThrownBy(() -> commands.delete(1L, 1L, 101L)).isInstanceOf(GeneralException.class);
        assertThat(event.getDeletedAt()).isNull();
    }

    @Test
    void deleteIsSoftAndUsesServerClock() {
        prepareUpdate(new EventActivity(0, false, 0L, "NOT_STARTED", false));
        commands.delete(1L, 1L, 101L);
        assertThat(event.getDeletedAt()).isEqualTo(now);
    }

    @Test
    void registrationIsClosedExactlyAtDeadline() {
        event.revise(new EventDetails("행사", null, "장소", now, now.plusSeconds(1), 20, now));
        when(repository.findByIdAndOrganizationIdAndDeletedAtIsNull(101L, 1L)).thenReturn(Optional.of(event));
        when(activityPort.getActivity(1L, 101L, 1L))
                .thenReturn(new EventActivity(0, false, 0L, "NOT_STARTED", false));
        var response = queries.detail(1L, 1L, 101L);
        assertThat(response.registrationStatus()).isEqualTo("CLOSED");
        assertThat(response.canApply()).isFalse();
    }

    private void prepareUpdate(EventActivity activity) {
        when(repository.findForUpdate(101L, 1L)).thenReturn(Optional.of(event));
        when(activityPort.getActivity(1L, 101L, 1L)).thenReturn(activity);
    }
}
