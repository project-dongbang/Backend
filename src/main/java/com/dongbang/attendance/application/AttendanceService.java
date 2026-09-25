package com.dongbang.attendance.application;

import com.dongbang.attendance.application.dto.AttendanceResults.*;
import com.dongbang.attendance.application.dto.ModifyAttendanceCommand;
import com.dongbang.attendance.application.event.AttendanceStartedEvent;
import com.dongbang.attendance.application.port.QrTokenGenerator;
import com.dongbang.attendance.domain.*;
import com.dongbang.attendance.exception.AttendanceErrorCode;
import com.dongbang.event.application.EventAccessService;
import com.dongbang.event.application.facade.EventAttendanceFacade;
import com.dongbang.event.application.facade.EventAttendanceFacade.AttendanceEvent;
import com.dongbang.finance.application.facade.AuditLogFacade;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.organization.application.facade.MembershipSummary;
import com.dongbang.organization.application.facade.ParticipantMemberSummary;
import com.dongbang.organization.exception.OrganizationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final AttendanceSessionRepository sessions;
    private final AttendanceRecordRepository records;
    private final EventAttendanceFacade events;
    private final EventAccessService access;
    private final MembershipAccessFacade memberships;
    private final QrTokenGenerator qrTokens;
    private final AuditLogFacade auditLog;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public EventList events(Long organizationId, Long userId, String keyword, LocalDate startDate,
                            LocalDate endDate, EventSort sort, int page, int size) {
        access.requireStaff(organizationId, userId);
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        }
        Comparator<AttendanceEvent> comparator = Comparator.comparing(AttendanceEvent::startsAt)
                .thenComparing(AttendanceEvent::eventId);
        if (sort == EventSort.START_DESC) comparator = comparator.reversed();
        String normalized = normalize(keyword);
        List<EventItem> items = events.events(organizationId).stream()
                .filter(e -> normalized == null || e.title().toLowerCase(Locale.ROOT).contains(normalized))
                .filter(e -> within(e.startsAt(), startDate, endDate))
                .sorted(comparator)
                .map(e -> new EventItem(e.eventId(), e.title(), e.startsAt(), e.endsAt(), e.location(),
                        events.participantIds(e.eventId()).size(), sessionStatus(sessions.findByEventId(e.eventId()).orElse(null), now())))
                .toList();
        Slice<EventItem> slice = slice(items, page, size);
        return new EventList(slice.content(), page, size, slice.total(), slice.pages(), slice.hasNext());
    }

    public Status status(Long organizationId, Long userId, Long eventId, RecordFilter filter,
                         String keyword, int page, int size) {
        access.requireStaff(organizationId, userId);
        AttendanceEvent event = events.event(organizationId, eventId);
        Instant now = now();
        AttendanceSession session = sessions.findByEventId(eventId).orElse(null);
        List<AttendanceItem> all = session == null ? initialRecords(organizationId, eventId)
                : persistedRecords(organizationId, session.getId());
        Summary summary = summary(all);
        String normalized = normalize(keyword);
        List<AttendanceItem> filtered = all.stream()
                .filter(r -> filter == RecordFilter.ALL || r.status().name().equals(filter.name()))
                .filter(r -> normalized == null || contains(r.memberName(), normalized) || contains(r.studentNumber(), normalized))
                .toList();
        Slice<AttendanceItem> slice = slice(filtered, page, size);
        return new Status(eventId, event.title(), now, sessionView(session, now), summary,
                slice.content(), page, size, slice.total(), slice.pages(), slice.hasNext());
    }

    @Transactional
    public Started start(Long organizationId, Long userId, Long eventId) {
        Long actorId = access.requireStaff(organizationId, userId);
        AttendanceEvent event = events.lockEvent(organizationId, eventId);
        if (sessions.findByEventId(eventId).isPresent()) throw new GeneralException(AttendanceErrorCode.ALREADY_GENERATED);
        Instant now = now();
        AttendanceSession session = sessions.save(new AttendanceSession(eventId, actorId, qrTokens.generate(), now));
        List<Long> participantIds = events.participantIds(eventId);
        for (Long membershipId : participantIds) {
            records.save(new AttendanceRecord(session.getId(), membershipId));
        }
        eventPublisher.publishEvent(new AttendanceStartedEvent(
                organizationId, eventId, session.getId(), event.title(), now, participantIds));
        return new Started(eventId, sessionView(session, now), now);
    }

    @Transactional
    public void close(Long organizationId, Long userId, Long eventId) {
        access.requireStaff(organizationId, userId);
        events.lockEvent(organizationId, eventId);
        AttendanceSession session = sessions.findForUpdateByEventId(eventId)
                .orElseThrow(() -> new GeneralException(AttendanceErrorCode.SESSION_NOT_STARTED));
        session.close(now());
    }

    @Transactional
    public void modify(Long organizationId, Long userId, Long eventId, Long attendanceId,
                       ModifyAttendanceCommand command) {
        Long actorId = access.requireStaff(organizationId, userId);
        events.event(organizationId, eventId);
        AttendanceSession session = sessions.findByEventId(eventId)
                .orElseThrow(() -> new GeneralException(AttendanceErrorCode.SESSION_NOT_STARTED));
        AttendanceRecord record = records.findForUpdateByIdAndAttendanceSessionId(attendanceId, session.getId())
                .orElseThrow(() -> new GeneralException(AttendanceErrorCode.RECORD_NOT_FOUND));
        if (record.getVersion() != command.version()) throw new GeneralException(AttendanceErrorCode.VERSION_CONFLICT);
        String before = snapshot(record);
        record.modify(command.status(), actorId, command.reason(), now());
        auditLog.record(organizationId, actorId, "ATTENDANCE_MODIFY", "ATTENDANCE_RECORD",
                record.getId(), before, snapshot(record), command.reason());
    }

    @Transactional
    public CheckIn checkIn(Long organizationId, Long userId, Long eventId, String qrToken) {
        access.requireMember(organizationId, userId);
        events.event(organizationId, eventId);
        Long membershipId = ownMembership(organizationId, userId);
        if (!events.isParticipant(eventId, membershipId)) throw new GeneralException(AttendanceErrorCode.PARTICIPANT_ONLY);
        AttendanceSession expected = sessions.findByEventId(eventId)
                .orElseThrow(() -> new GeneralException(AttendanceErrorCode.SESSION_NOT_STARTED));
        AttendanceSession session = sessions.findByQrToken(qrToken)
                .orElseThrow(() -> new GeneralException(AttendanceErrorCode.INVALID_QR));
        if (!session.getId().equals(expected.getId())) throw new GeneralException(AttendanceErrorCode.DIFFERENT_EVENT_QR);
        Instant now = now();
        if (session.getStatus() == AttendanceSessionStatus.CLOSED) throw new GeneralException(AttendanceErrorCode.SESSION_CLOSED);
        if (session.isExpired(now)) throw new GeneralException(AttendanceErrorCode.QR_EXPIRED);
        AttendanceRecord record = records.findForUpdate(session.getId(), membershipId)
                .orElseThrow(() -> new GeneralException(AttendanceErrorCode.PARTICIPANT_ONLY));
        if (record.getStatus() == AttendanceStatus.PRESENT) throw new GeneralException(AttendanceErrorCode.ALREADY_PRESENT);
        record.checkIn(now);
        return new CheckIn(eventId, record.getId(), membershipId, record.getStatus(),
                record.getCheckedAt(), record.getSource());
    }

    public MyList mine(Long organizationId, Long userId, int page, int size) {
        access.requireMember(organizationId, userId);
        Long membershipId = ownMembership(organizationId, userId);
        Instant now = now();
        List<MyRecord> all = events.registeredEvents(organizationId, membershipId).stream()
                .sorted(Comparator.comparing(AttendanceEvent::startsAt).reversed())
                .map(event -> myRecord(event, membershipId, now))
                .toList();
        long present = all.stream().filter(r -> r.displayStatus() == DisplayStatus.PRESENT).count();
        long absent = all.stream().filter(r -> r.displayStatus() == DisplayStatus.ABSENT).count();
        long waiting = all.size() - present - absent;
        Slice<MyRecord> slice = slice(all, page, size);
        return new MyList(new MySummary(all.size(), present, absent, waiting), slice.content(), page, size,
                slice.total(), slice.pages(), slice.hasNext());
    }

    private MyRecord myRecord(AttendanceEvent event, Long membershipId, Instant now) {
        AttendanceSession session = sessions.findByEventId(event.eventId()).orElse(null);
        AttendanceRecord record = session == null ? null
                : records.findByAttendanceSessionIdAndMembershipId(session.getId(), membershipId).orElse(null);
        AttendanceStatus status = record == null ? AttendanceStatus.ABSENT : record.getStatus();
        SessionViewStatus sessionStatus = sessionStatus(session, now);
        DisplayStatus display = status == AttendanceStatus.PRESENT ? DisplayStatus.PRESENT
                : (sessionStatus == SessionViewStatus.CLOSED || sessionStatus == SessionViewStatus.EXPIRED)
                ? DisplayStatus.ABSENT : DisplayStatus.WAITING;
        return new MyRecord(event.eventId(), event.title(), event.startsAt(), event.endsAt(), event.location(), true,
                status, display, record == null ? null : record.getCheckedAt(), record == null ? null : record.getSource());
    }

    private List<AttendanceItem> initialRecords(Long organizationId, Long eventId) {
        Map<Long, ParticipantMemberSummary> members = memberMap(organizationId);
        return events.participantIds(eventId).stream().map(id -> toRecord(null, id, members.get(id))).toList();
    }

    private List<AttendanceItem> persistedRecords(Long organizationId, Long sessionId) {
        Map<Long, ParticipantMemberSummary> members = memberMap(organizationId);
        return records.findByAttendanceSessionId(sessionId).stream()
                .sorted(Comparator.comparing(AttendanceRecord::getId))
                .map(r -> toRecord(r, r.getMembershipId(), members.get(r.getMembershipId()))).toList();
    }

    private AttendanceItem toRecord(AttendanceRecord record, Long membershipId, ParticipantMemberSummary member) {
        return new AttendanceItem(record == null ? null : record.getId(), membershipId,
                member == null ? "탈퇴 회원" : member.memberName(), member == null ? null : member.studentNumber(),
                record == null ? AttendanceStatus.ABSENT : record.getStatus(),
                record == null ? null : record.getCheckedAt(), record == null ? null : record.getSource(),
                record == null ? null : record.getVersion());
    }

    private Summary summary(List<AttendanceItem> all) {
        long present = all.stream().filter(r -> r.status() == AttendanceStatus.PRESENT).count();
        long total = all.size();
        double rate = total == 0 ? 0.0 : Math.round((present * 1000.0 / total)) / 10.0;
        return new Summary(total, present, total - present, rate);
    }

    private Session sessionView(AttendanceSession session, Instant now) {
        if (session == null) return new Session(null, SessionViewStatus.NOT_STARTED, null, null, null, null);
        SessionViewStatus status = sessionStatus(session, now);
        return new Session(session.getId(), status, session.getOpenedAt(), session.expiresAt(), session.getClosedAt(),
                status == SessionViewStatus.ACTIVE ? session.getQrToken() : null);
    }

    private SessionViewStatus sessionStatus(AttendanceSession session, Instant now) {
        if (session == null) return SessionViewStatus.NOT_STARTED;
        if (session.getStatus() == AttendanceSessionStatus.CLOSED) return SessionViewStatus.CLOSED;
        return session.isExpired(now) ? SessionViewStatus.EXPIRED : SessionViewStatus.ACTIVE;
    }

    private Map<Long, ParticipantMemberSummary> memberMap(Long organizationId) {
        return memberships.getParticipantMembers(organizationId).stream()
                .collect(Collectors.toMap(ParticipantMemberSummary::membershipId, Function.identity()));
    }

    private Long ownMembership(Long organizationId, Long userId) {
        return memberships.getMembershipSummary(organizationId, userId).map(MembershipSummary::membershipId)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.MEMBER_REQUIRED));
    }

    private boolean within(Instant instant, LocalDate start, LocalDate end) {
        LocalDate date = instant.atZone(SEOUL).toLocalDate();
        return (start == null || !date.isBefore(start)) && (end == null || !date.isAfter(end));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean contains(String value, String normalized) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalized);
    }

    private Instant now() { return clock.instant(); }
    private String snapshot(AttendanceRecord r) {
        return "{\"status\":\"" + r.getStatus() + "\",\"source\":"
                + (r.getSource() == null ? "null" : "\"" + r.getSource() + "\"") + "}";
    }

    private <T> Slice<T> slice(List<T> all, int page, int size) {
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        int pages = all.isEmpty() ? 0 : (all.size() + size - 1) / size;
        return new Slice<>(all.subList(from, to), all.size(), pages, page + 1 < pages);
    }

    private record Slice<T>(List<T> content, long total, int pages, boolean hasNext) {}
}
