package com.dongbang.attendance.infrastructure.persistence;

import com.dongbang.attendance.domain.AttendanceRecord;
import com.dongbang.attendance.domain.AttendanceRecordRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.Instant;

public interface AttendanceRecordJpaRepository
        extends JpaRepository<AttendanceRecord, Long>, AttendanceRecordRepository {
    @Override
    @Query("select r from AttendanceRecord r where r.attendanceSessionId = :sessionId and r.targetActive = true order by r.id")
    java.util.List<AttendanceRecord> findByAttendanceSessionId(@Param("sessionId") Long sessionId);

    @Override
    @Query("select r from AttendanceRecord r where r.id = :id and r.attendanceSessionId = :sessionId and r.targetActive = true")
    Optional<AttendanceRecord> findByIdAndAttendanceSessionId(@Param("id") Long id,
                                                              @Param("sessionId") Long sessionId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AttendanceRecord r where r.id = :id and r.attendanceSessionId = :sessionId and r.targetActive = true")
    Optional<AttendanceRecord> findForUpdateByIdAndAttendanceSessionId(@Param("id") Long id,
                                                                       @Param("sessionId") Long sessionId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AttendanceRecord r where r.attendanceSessionId = :sessionId and r.membershipId = :membershipId and r.targetActive = true")
    Optional<AttendanceRecord> findForUpdate(@Param("sessionId") Long sessionId,
                                             @Param("membershipId") Long membershipId);

    @Query("""
            select new com.dongbang.attendance.infrastructure.persistence.DashboardAttendanceTotals(
                count(r), coalesce(sum(case when r.status = com.dongbang.attendance.domain.AttendanceStatus.PRESENT
                    then 1L else 0L end), 0L))
            from AttendanceRecord r, AttendanceSession s, Event e
            where r.attendanceSessionId = s.id and s.eventId = e.id
              and e.organizationId = :organizationId and e.deletedAt is null
              and e.status = com.dongbang.event.domain.EventStatus.SCHEDULED
              and r.targetActive = true
              and (s.status = com.dongbang.attendance.domain.AttendanceSessionStatus.CLOSED
                   or s.openedAt <= :expiredBefore)
            """)
    DashboardAttendanceTotals dashboardTotals(@Param("organizationId") Long organizationId,
                                               @Param("expiredBefore") Instant expiredBefore);

    @Query("""
            select new com.dongbang.attendance.infrastructure.persistence.DashboardAttendanceTotals(
                count(r), coalesce(sum(case when r.status = com.dongbang.attendance.domain.AttendanceStatus.PRESENT
                    then 1L else 0L end), 0L))
            from AttendanceRecord r, AttendanceSession s, Event e
            where r.attendanceSessionId = s.id and s.eventId = e.id
              and e.organizationId = :organizationId and e.deletedAt is null
              and e.status = com.dongbang.event.domain.EventStatus.SCHEDULED
              and r.membershipId = :membershipId and r.targetActive = true
              and (s.status = com.dongbang.attendance.domain.AttendanceSessionStatus.CLOSED
                   or s.openedAt <= :expiredBefore)
            """)
    DashboardAttendanceTotals dashboardMemberTotals(@Param("organizationId") Long organizationId,
                                                     @Param("membershipId") Long membershipId,
                                                     @Param("expiredBefore") Instant expiredBefore);
}
