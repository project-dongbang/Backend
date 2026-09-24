package com.dongbang.attendance.application.dto;

import com.dongbang.attendance.domain.AttendanceSource;
import com.dongbang.attendance.domain.AttendanceStatus;

import java.time.Instant;
import java.util.List;

public final class AttendanceResults {
    private AttendanceResults() {}

    public record EventList(List<EventItem> events, int page, int size, long totalElements,
                            int totalPages, boolean hasNext) {}
    public record EventItem(Long eventId, String title, Instant startsAt, Instant endsAt, String location,
                            int participantCount, SessionViewStatus attendanceSessionStatus) {}

    public record Status(Long eventId, String title, Instant serverTime, Session session, Summary summary,
                         List<AttendanceItem> records, int page, int size, long totalElements,
                         int totalPages, boolean hasNext) {}
    public record Session(Long attendanceSessionId, SessionViewStatus status, Instant startedAt,
                          Instant expiresAt, Instant closedAt, String qrToken) {}
    public record Summary(long totalCount, long presentCount, long absentCount, double attendanceRate) {}
    public record AttendanceItem(Long attendanceId, Long membershipId, String memberName, String studentNumber,
                                 AttendanceStatus status, Instant checkedInAt, AttendanceSource method, Long version) {}

    public record Started(Long eventId, Session session, Instant serverTime) {}
    public record CheckIn(Long eventId, Long attendanceId, Long membershipId, AttendanceStatus status,
                          Instant checkedInAt, AttendanceSource method) {}

    public record MyList(MySummary summary, List<MyRecord> records, int page, int size,
                         long totalElements, int totalPages, boolean hasNext) {}
    public record MySummary(long eventCount, long presentCount, long absentCount, long waitingCount) {}
    public record MyRecord(Long eventId, String title, Instant startsAt, Instant endsAt, String location,
                           boolean participating, AttendanceStatus status, DisplayStatus displayStatus,
                           Instant checkedInAt, AttendanceSource method) {}

    public enum SessionViewStatus { ACTIVE, EXPIRED, CLOSED, NOT_STARTED }
    public enum DisplayStatus { WAITING, PRESENT, ABSENT }
    public enum RecordFilter { ALL, PRESENT, ABSENT }
    public enum EventSort { START_ASC, START_DESC }
}
