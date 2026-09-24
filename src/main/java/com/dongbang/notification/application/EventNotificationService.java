package com.dongbang.notification.application;

import com.dongbang.attendance.application.event.AttendanceStartedEvent;
import com.dongbang.event.application.event.ScheduleCreatedEvent;
import com.dongbang.event.domain.Event;
import com.dongbang.event.domain.repository.EventRepository;
import com.dongbang.notification.domain.Notification;
import com.dongbang.notification.domain.NotificationReferenceType;
import com.dongbang.notification.domain.NotificationType;
import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.organization.domain.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventNotificationService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M월 d일");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final EventRepository eventRepository;
    private final MembershipRepository membershipRepository;
    private final NotificationCreator notificationCreator;

    @Transactional
    public int createScheduleCreated(ScheduleCreatedEvent event) {
        String message = "%s %s 일정이 등록되었습니다.".formatted(
                DATE.format(event.startsAt().atZone(SEOUL)), event.title());
        List<Notification> notifications = activeLinkedMembers(event.organizationId()).stream()
                .map(member -> Notification.create(
                        event.organizationId(), member.getUserId(), NotificationType.SCHEDULE_CREATED,
                        "새 일정이 등록되었습니다.", message, NotificationReferenceType.EVENT,
                        event.eventId(), "SCHEDULE_CREATED:%d:%d".formatted(event.eventId(), member.getUserId())))
                .toList();
        return notificationCreator.saveNew(notifications);
    }

    @Transactional
    public int createAttendanceStarted(AttendanceStartedEvent event) {
        Set<Long> participantIds = Set.copyOf(event.participantMembershipIds());
        List<Notification> notifications = membershipRepository.findAllByIdIn(participantIds).stream()
                .filter(member -> member.getOrganization().getId().equals(event.organizationId()))
                .filter(this::isActiveAndLinked)
                .map(member -> Notification.create(
                        event.organizationId(), member.getUserId(), NotificationType.ATTENDANCE_STARTED,
                        "출석체크가 시작되었습니다.", "%s 출석체크가 시작되었습니다.".formatted(event.eventTitle()),
                        NotificationReferenceType.ATTENDANCE_SESSION, event.attendanceSessionId(),
                        "ATTENDANCE_STARTED:%d:%d".formatted(event.attendanceSessionId(), member.getUserId())))
                .toList();
        return notificationCreator.saveNew(notifications);
    }

    @Transactional
    public int createEventDayBeforeReminders(LocalDate eventDate) {
        Instant from = eventDate.atStartOfDay(SEOUL).toInstant();
        Instant until = eventDate.plusDays(1).atStartOfDay(SEOUL).toInstant();
        return eventRepository.findScheduledEventsStartingBetween(from, until).stream()
                .mapToInt(event -> notificationCreator.saveNew(dayBeforeNotifications(event, eventDate)))
                .sum();
    }

    private List<Notification> dayBeforeNotifications(Event event, LocalDate eventDate) {
        String message = "%s 행사가 내일 %s에 진행됩니다.".formatted(
                event.getTitle(), TIME.format(event.getStartsAt().atZone(SEOUL)));
        return activeLinkedMembers(event.getOrganizationId()).stream()
                .map(member -> Notification.create(
                        event.getOrganizationId(), member.getUserId(), NotificationType.EVENT_DAY_BEFORE_REMINDER,
                        "내일 예정된 행사가 있습니다.", message, NotificationReferenceType.EVENT, event.getId(),
                        "EVENT_DAY_BEFORE_REMINDER:%d:%s:%d".formatted(
                                event.getId(), eventDate, member.getUserId())))
                .toList();
    }

    private List<Membership> activeLinkedMembers(Long organizationId) {
        return membershipRepository.findAllByOrganizationIdAndStatus(organizationId, MembershipStatus.ACTIVE)
                .stream()
                .filter(this::isActiveAndLinked)
                .toList();
    }

    private boolean isActiveAndLinked(Membership membership) {
        return membership.getStatus() == MembershipStatus.ACTIVE && membership.getUserId() != null;
    }
}
