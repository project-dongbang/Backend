package com.dongbang.attendance.domain;

import java.util.Optional;

public interface AttendanceSessionRepository {
    AttendanceSession save(AttendanceSession session);
    Optional<AttendanceSession> findByEventId(Long eventId);
    Optional<AttendanceSession> findForUpdateByEventId(Long eventId);
    Optional<AttendanceSession> findByQrToken(String qrToken);
}
