package com.dongbang.event.application;

import com.dongbang.event.domain.*;
import com.dongbang.event.domain.repository.*;
import com.dongbang.event.exception.EventErrorCode;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.application.facade.*;
import com.dongbang.organization.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class EventParticipationServiceTest {
    @Mock EventRepository events;
    @Mock EventParticipantRepository participants;
    @Mock EventAccessService access;
    @Mock MembershipAccessFacade memberships;
    @Mock com.dongbang.finance.application.facade.AuditLogFacade auditLog;
    final Instant now = Instant.parse("2026-09-24T00:00:00Z");
    EventParticipationService service;
    Event event;
    @BeforeEach void setup() {
        service = new EventParticipationService(events, participants, access, memberships, Clock.fixed(now, ZoneOffset.UTC), auditLog);
        event = Event.builder().id(10L).organizationId(1L).createdByMembershipId(2L).type(EventType.EVENT)
                .details(new EventDetails("행사", null, "장소", now.plusSeconds(3600), now.plusSeconds(7200), 1, null)).build();
    }
    void locked() { when(events.findForUpdate(10L, 1L)).thenReturn(Optional.of(event)); }
    void member() {
        when(memberships.getMembershipSummary(1L, 3L)).thenReturn(Optional.of(
                new MembershipSummary(2L, 1L, 3L, "회원", MembershipRole.MEMBER, MembershipStatus.ACTIVE)));
    }
    @Test void applyPersistsAndAdvancesVersion() {
        locked(); member();
        service.apply(1L, 3L, 10L);
        verify(access).requireMember(1L, 3L);
        verify(participants).save(argThat(p -> p.getEventId().equals(10L) && p.getMembershipId().equals(2L)));
        assertThat(event.getParticipantVersion()).isEqualTo(1);
    }
    @Test void capacityIsEnforced() {
        locked(); member(); when(participants.countByEventId(10L)).thenReturn(1);
        assertError(() -> service.apply(1L, 3L, 10L), EventErrorCode.REGISTRATION_FULL);
        verify(participants, never()).save(any());
    }
    @Test void duplicateIsRejected() {
        locked(); member();
        when(participants.findByEventIdAndMembershipId(10L, 2L)).thenReturn(Optional.of(new EventParticipant(10L, 2L, now)));
        assertError(() -> service.apply(1L, 3L, 10L), EventErrorCode.ALREADY_PARTICIPATING);
    }
    @Test void earlyClosureBlocksApplyAndWithdraw() {
        locked(); event.closeRegistration(now);
        assertError(() -> service.apply(1L, 3L, 10L), EventErrorCode.REGISTRATION_CLOSED);
        assertError(() -> service.withdraw(1L, 3L, 10L), EventErrorCode.REGISTRATION_CLOSED);
    }
    @Test void deadlineBoundaryIsClosed() {
        locked();
        service = new EventParticipationService(events, participants, access, memberships,
                Clock.fixed(event.getStartsAt(), ZoneOffset.UTC), auditLog);
        assertError(() -> service.apply(1L, 3L, 10L), EventErrorCode.REGISTRATION_CLOSED);
    }
    @Test void withdrawFreesSeatAndChangesVersion() {
        locked(); member();
        var participant = new EventParticipant(10L, 2L, now);
        when(participants.findByEventIdAndMembershipId(10L, 2L)).thenReturn(Optional.of(participant));
        service.withdraw(1L, 3L, 10L);
        verify(participants, never()).delete(any());
        assertThat(participant.getStatus()).isEqualTo("CANCELED");
        assertThat(participant.getCanceledAt()).isEqualTo(now);
        assertThat(event.getParticipantVersion()).isEqualTo(1);
    }
    @Test void staffEditRequiresCurrentVersion() {
        locked(); event.participantsChanged();
        assertError(() -> service.changeParticipants(1L, 3L, 10L,
                new com.dongbang.event.application.command.ChangeParticipantsCommand(0, java.util.List.of())),
                EventErrorCode.VERSION_CONFLICT);
        verify(access).requireStaff(1L, 3L);
        verifyNoInteractions(participants, memberships);
    }
    @Test void scheduleCannotAcceptParticipants() {
        event = Event.builder().id(10L).organizationId(1L).type(EventType.SCHEDULE)
                .details(new EventDetails("일정", null, "장소", now, now.plusSeconds(1), null, null)).build();
        locked(); assertError(() -> service.apply(1L, 3L, 10L), EventErrorCode.EVENT_ONLY);
    }
    @Test void deniedAccessNeverTouchesEvent() {
        doThrow(new GeneralException(com.dongbang.organization.exception.OrganizationErrorCode.MEMBER_REQUIRED))
                .when(access).requireMember(1L, 3L);
        assertThatThrownBy(() -> service.apply(1L, 3L, 10L)).isInstanceOf(GeneralException.class);
        verifyNoInteractions(events, participants);
    }
    private void assertError(Runnable action, EventErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(GeneralException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(code));
    }

    @Test void batchReplacementAtCapacityChecksFinalCount() {
        locked();
        var previous = new EventParticipant(10L, 2L, now);
        when(participants.findByEventIdOrderByRegisteredAtAscIdAsc(10L)).thenReturn(java.util.List.of(previous));
        when(memberships.getMembershipSummaryById(4L)).thenReturn(Optional.of(
                new MembershipSummary(4L, 1L, 5L, "추가 회원", MembershipRole.MEMBER, MembershipStatus.ACTIVE)));
        var result = service.changeParticipants(1L, 3L, 10L,
                new com.dongbang.event.application.command.ChangeParticipantsCommand(0, java.util.List.of(
                        new com.dongbang.event.application.command.ChangeParticipantsCommand.Change(4L, ParticipantAction.ADD),
                        new com.dongbang.event.application.command.ChangeParticipantsCommand.Change(2L, ParticipantAction.REMOVE))));
        assertThat(result.participantCount()).isEqualTo(1);
        assertThat(result.participantVersion()).isEqualTo(1);
        assertThat(previous.getStatus()).isEqualTo("CANCELED");
        verify(participants).save(argThat(p -> p.getMembershipId().equals(4L)));
    }

    @Test void batchOverflowDoesNotModifyRegistrationsOrVersion() {
        locked();
        var previous = new EventParticipant(10L, 2L, now);
        when(participants.findByEventIdOrderByRegisteredAtAscIdAsc(10L)).thenReturn(java.util.List.of(previous));
        when(memberships.getMembershipSummaryById(4L)).thenReturn(Optional.of(
                new MembershipSummary(4L, 1L, 5L, "추가 회원", MembershipRole.MEMBER, MembershipStatus.ACTIVE)));
        assertError(() -> service.changeParticipants(1L, 3L, 10L,
                new com.dongbang.event.application.command.ChangeParticipantsCommand(0, java.util.List.of(
                        new com.dongbang.event.application.command.ChangeParticipantsCommand.Change(4L, ParticipantAction.ADD)))),
                EventErrorCode.CAPACITY_EXCEEDED);
        verify(participants, never()).save(any());
        assertThat(previous.getStatus()).isEqualTo("REGISTERED");
        assertThat(event.getParticipantVersion()).isZero();
        verifyNoInteractions(auditLog);
    }

    @Test void reapplicationReusesCanceledRegistration() {
        locked(); member();
        var previous = new EventParticipant(10L, 2L, now.minusSeconds(100));
        previous.cancel(now.minusSeconds(50));
        when(participants.findRegistration(10L, 2L)).thenReturn(Optional.of(previous));
        service.apply(1L, 3L, 10L);
        assertThat(previous.getStatus()).isEqualTo("REGISTERED");
        assertThat(previous.getCanceledAt()).isNull();
        verify(participants, never()).save(any());
    }

    @Test void duplicateBatchTargetDoesNotChangeState() {
        locked();
        var previous = new EventParticipant(10L, 2L, now);
        when(participants.findByEventIdOrderByRegisteredAtAscIdAsc(10L)).thenReturn(java.util.List.of(previous));
        var change = new com.dongbang.event.application.command.ChangeParticipantsCommand.Change(2L, ParticipantAction.REMOVE);
        assertError(() -> service.changeParticipants(1L, 3L, 10L,
                new com.dongbang.event.application.command.ChangeParticipantsCommand(0, java.util.List.of(change, change))),
                EventErrorCode.INVALID_PARTICIPANT_CHANGE);
        assertThat(previous.getStatus()).isEqualTo("REGISTERED");
        assertThat(event.getParticipantVersion()).isZero();
        verifyNoInteractions(auditLog);
    }

    @Test void staffCanRemoveParticipantAfterRegistrationCloses() {
        locked();
        event.closeRegistration(now.minusSeconds(1));
        var previous = new EventParticipant(10L, 2L, now.minusSeconds(100));
        when(participants.findByEventIdOrderByRegisteredAtAscIdAsc(10L)).thenReturn(java.util.List.of(previous));
        var result = service.changeParticipants(1L, 3L, 10L,
                new com.dongbang.event.application.command.ChangeParticipantsCommand(0, java.util.List.of(
                        new com.dongbang.event.application.command.ChangeParticipantsCommand.Change(2L, ParticipantAction.REMOVE))));
        assertThat(result.participantCount()).isZero();
        assertThat(previous.getStatus()).isEqualTo("CANCELED");
    }
}
