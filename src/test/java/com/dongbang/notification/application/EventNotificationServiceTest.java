package com.dongbang.notification.application;

import com.dongbang.attendance.application.event.AttendanceStartedEvent;
import com.dongbang.event.application.event.ScheduleCreatedEvent;
import com.dongbang.event.domain.Event;
import com.dongbang.event.domain.EventDetails;
import com.dongbang.event.domain.EventType;
import com.dongbang.event.domain.repository.EventRepository;
import com.dongbang.notification.domain.Notification;
import com.dongbang.notification.domain.NotificationType;
import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.MembershipRole;
import com.dongbang.organization.domain.Organization;
import com.dongbang.organization.domain.repository.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventNotificationServiceTest {
    @Mock EventRepository eventRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock NotificationCreator notificationCreator;

    private EventNotificationService service;
    private Organization organization;
    private Membership linked;
    private Membership unlinked;

    @BeforeEach
    void setUp() {
        service = new EventNotificationService(eventRepository, membershipRepository, notificationCreator);
        organization = Organization.builder().id(1L).name("동방").slug("dongbang").build();
        linked = Membership.builder().id(11L).organization(organization).userId(21L)
                .memberName("회원").studentNumber("20260001").role(MembershipRole.MEMBER).build();
        unlinked = Membership.builder().id(12L).organization(organization)
                .memberName("미연결").studentNumber("20260002").role(MembershipRole.MEMBER).build();
    }

    @Test
    void scheduleNotificationIsSentOnlyToLinkedActiveMembers() {
        given(membershipRepository.findAllByOrganizationIdAndStatus(
                1L, com.dongbang.organization.domain.MembershipStatus.ACTIVE))
                .willReturn(List.of(linked, unlinked));
        given(notificationCreator.saveNew(anyList())).willReturn(1);

        int count = service.createScheduleCreated(new ScheduleCreatedEvent(
                1L, 31L, "운영진 회의", Instant.parse("2026-09-25T10:00:00Z")));

        assertThat(count).isEqualTo(1);
        assertSingleNotification(NotificationType.SCHEDULE_CREATED, 21L, 31L);
    }

    @Test
    void attendanceNotificationUsesOnlyRegisteredParticipantMemberships() {
        given(membershipRepository.findAllByIdIn(java.util.Set.of(11L, 12L)))
                .willReturn(List.of(linked, unlinked));
        given(notificationCreator.saveNew(anyList())).willReturn(1);

        service.createAttendanceStarted(new AttendanceStartedEvent(
                1L, 31L, 41L, "개강 총회", Instant.parse("2026-09-25T10:00:00Z"), List.of(11L, 12L)));

        assertSingleNotification(NotificationType.ATTENDANCE_STARTED, 21L, 41L);
    }

    @Test
    void dayBeforeReminderUsesSeoulDateRangeAndAllLinkedMembers() {
        LocalDate eventDate = LocalDate.of(2026, 9, 26);
        Event event = Event.builder().id(31L).organizationId(1L).createdByMembershipId(9L)
                .type(EventType.EVENT)
                .details(new EventDetails("환영 행사", null, "동아리방",
                        Instant.parse("2026-09-26T07:00:00Z"), Instant.parse("2026-09-26T09:00:00Z"), null, null))
                .build();
        given(eventRepository.findScheduledEventsStartingBetween(
                Instant.parse("2026-09-25T15:00:00Z"), Instant.parse("2026-09-26T15:00:00Z")))
                .willReturn(List.of(event));
        given(membershipRepository.findAllByOrganizationIdAndStatus(
                1L, com.dongbang.organization.domain.MembershipStatus.ACTIVE)).willReturn(List.of(linked));
        given(notificationCreator.saveNew(anyList())).willReturn(1);

        assertThat(service.createEventDayBeforeReminders(eventDate)).isEqualTo(1);

        assertSingleNotification(NotificationType.EVENT_DAY_BEFORE_REMINDER, 21L, 31L);
    }

    private void assertSingleNotification(NotificationType type, Long userId, Long referenceId) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationCreator).saveNew(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(notification -> {
            assertThat(notification.getType()).isEqualTo(type);
            assertThat(notification.getUserId()).isEqualTo(userId);
            assertThat(notification.getReferenceId()).isEqualTo(referenceId);
        });
    }
}
