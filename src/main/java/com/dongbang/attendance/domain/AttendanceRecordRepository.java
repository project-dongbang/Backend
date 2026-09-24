package com.dongbang.attendance.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository {
    AttendanceRecord save(AttendanceRecord record);
    List<AttendanceRecord> findByAttendanceSessionId(Long sessionId);
    Optional<AttendanceRecord> findByIdAndAttendanceSessionId(Long id, Long sessionId);
    Optional<AttendanceRecord> findForUpdateByIdAndAttendanceSessionId(Long id, Long sessionId);
    Optional<AttendanceRecord> findByAttendanceSessionIdAndMembershipId(Long sessionId, Long membershipId);
    Optional<AttendanceRecord> findForUpdate(Long sessionId, Long membershipId);
    List<AttendanceRecord> findByMembershipIdAndAttendanceSessionIdIn(Long membershipId, Collection<Long> sessionIds);
}
