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
    final Instant now = Instant.parse("2026-09-24T00:00:00Z");
    EventParticipationService service;
    Event event;
    @BeforeEach void setup() {
        service = new EventParticipationService(events, participants, access, memberships, Clock.fixed(now, ZoneOffset.UTC));
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
        assertError(() -> service.apply(1L, 3L, 10L), EventErrorCode.CAPACITY_EXCEEDED);
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
                Clock.fixed(event.getStartsAt(), ZoneOffset.UTC));
        assertError(() -> service.apply(1L, 3L, 10L), EventErrorCode.REGISTRATION_CLOSED);
    }
    @Test void withdrawFreesSeatAndChangesVersion() {
        locked(); member();
        var participant = new EventParticipant(10L, 2L, now);
        when(participants.findByEventIdAndMembershipId(10L, 2L)).thenReturn(Optional.of(participant));
        service.withdraw(1L, 3L, 10L);
        verify(participants).delete(participant);
        assertThat(event.getParticipantVersion()).isEqualTo(1);
    }
    @Test void staffEditRequiresCurrentVersion() {
        locked(); event.participantsChanged();
        assertError(() -> service.addParticipant(1L, 3L, 10L, 2L, 0), EventErrorCode.VERSION_CONFLICT);
        verify(access).requireStaff(1L, 3L);
        verifyNoInteractions(participants, memberships);
    }
    @Test void otherOrganizationMemberCannotBeAdded() {
        locked();
        when(memberships.getMembershipSummaryById(2L)).thenReturn(Optional.of(
                new MembershipSummary(2L, 99L, 3L, "회원", MembershipRole.MEMBER, MembershipStatus.ACTIVE)));
        assertThatThrownBy(() -> service.addParticipant(1L, 3L, 10L, 2L, 0)).isInstanceOf(GeneralException.class);
        verifyNoInteractions(participants);
    }
    @Test void cancellationIsIdempotentAndBlocksApplications() {
        locked(); service.cancel(1L, 3L, 10L); service.cancel(1L, 3L, 10L);
        assertThat(event.getCanceledAt()).isEqualTo(now);
        assertError(() -> service.apply(1L, 3L, 10L), EventErrorCode.EVENT_CANCELED);
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
}
