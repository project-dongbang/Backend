package com.dongbang.event.application;

import com.dongbang.event.application.port.EventActivity;
import com.dongbang.event.application.port.EventActivityPort;
import com.dongbang.event.domain.Event;
import com.dongbang.event.domain.EventStatus;
import com.dongbang.event.domain.EventType;
import com.dongbang.event.domain.repository.EventRepository;
import com.dongbang.event.exception.EventErrorCode;
import com.dongbang.event.application.result.CalendarEventResult;
import com.dongbang.event.application.result.CalendarItemType;
import com.dongbang.event.application.result.CalendarResult;
import com.dongbang.event.application.result.EventDetailResult;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.finance.application.facade.FeeCalendarFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventQueryService {

    private static final ZoneId CALENDAR_ZONE = ZoneId.of("Asia/Seoul");

    private final EventRepository eventRepository;
    private final EventAccessService accessService;
    private final EventActivityPort activityPort;
    private final FeeCalendarFacade feeCalendarFacade;
    private final Clock clock;

    public CalendarResult calendar(Long organizationId, Long userId, int year, int month) {
        accessService.requireMember(organizationId, userId);
        if (year < 1 || year > 9999 || month < 1 || month > 12) {
            throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        }
        YearMonth target = YearMonth.of(year, month);
        LocalDate fromDate = target.atDay(1).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
        LocalDate toDate = target.atEndOfMonth().with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SATURDAY));
        Instant from = fromDate.atStartOfDay(CALENDAR_ZONE).toInstant();
        Instant until = toDate.plusDays(1).atStartOfDay(CALENDAR_ZONE).toInstant();
        var eventItems = eventRepository.findOverlapping(organizationId, from, until).stream()
                .map(this::toCalendar);
        var feeItems = feeCalendarFacade.findDeadlines(organizationId, fromDate, toDate).stream()
                .map(this::toCalendar);
        var events = Stream.concat(eventItems, feeItems)
                .sorted(Comparator.comparing(CalendarEventResult::startsAt)
                        .thenComparing(item -> item.eventId() == null ? item.feeItemId() : item.eventId()))
                .toList();
        return new CalendarResult(year, month, events);
    }

    public EventDetailResult detail(Long organizationId, Long userId, Long eventId) {
        accessService.requireMember(organizationId, userId);
        Event event = eventRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(eventId, organizationId)
                .orElseThrow(() -> new GeneralException(EventErrorCode.EVENT_NOT_FOUND));
        EventActivity activity = activityPort.getActivity(organizationId, eventId, userId);
        // 일반 일정의 행사 전용 응답 필드 제외
        boolean isEvent = event.getType() == EventType.EVENT;
        String registrationStatus = isEvent ? registrationStatus(event, activity, clock.instant()) : null;
        return new EventDetailResult(
                event.getId(), event.getOrganizationId(), event.getType(), event.getStatus(), event.getTitle(),
                event.getStartsAt(), event.getEndsAt(), event.getLocation(), event.getDescription(),
                event.getCapacity(), event.getRegistrationDeadline(), registrationStatus,
                isEvent ? activity.participantCount() : null,
                isEvent ? activity.participating() : null,
                isEvent ? "OPEN".equals(registrationStatus) && !activity.participating() : null,
                // TODO(policy): 신청 마감 전 본인 취소 허용 여부 확정 필요
                isEvent ? !"CLOSED".equals(registrationStatus) && activity.participating() : null,
                isEvent ? activity.participantVersion() : null,
                isEvent ? activity.attendanceSessionStatus() : null);
    }

    private CalendarEventResult toCalendar(Event event) {
        return new CalendarEventResult(
                event.getId(), null, CalendarItemType.valueOf(event.getType().name()),
                event.getTitle(), event.getStartsAt(), event.getEndsAt(), null);
    }

    private CalendarEventResult toCalendar(FeeCalendarFacade.FeeCalendarItem fee) {
        Instant deadline = fee.dueDate().atTime(LocalTime.of(23, 59)).atZone(CALENDAR_ZONE).toInstant();
        return new CalendarEventResult(
                null, fee.feeItemId(), CalendarItemType.FEE_DUE, fee.title(), deadline, deadline, fee.dueDate());
    }

    private String registrationStatus(Event event, EventActivity activity, Instant now) {
        // 정원 충족보다 신청 마감 상태 우선
        if (event.getStatus() != EventStatus.SCHEDULED || activity.registrationClosedEarly()
                || !now.isBefore(event.getRegistrationDeadline())
                || (event.getRegistrationOpensAt() != null && now.isBefore(event.getRegistrationOpensAt()))) {
            return "CLOSED";
        }
        return event.getCapacity() != null && activity.participantCount() >= event.getCapacity() ? "FULL" : "OPEN";
    }
}
