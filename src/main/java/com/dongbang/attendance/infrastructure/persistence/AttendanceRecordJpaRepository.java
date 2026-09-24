package com.dongbang.attendance.infrastructure.persistence;

import com.dongbang.attendance.domain.AttendanceRecord;
import com.dongbang.attendance.domain.AttendanceRecordRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

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
    @Query("select r from AttendanceRecord r where r.attendanceSessionId = :sessionId and r.membershipId = :membershipId")
    Optional<AttendanceRecord> findForUpdate(@Param("sessionId") Long sessionId,
                                             @Param("membershipId") Long membershipId);
}
