package com.dongbang.attendance.infrastructure.persistence;

import com.dongbang.attendance.domain.AttendanceSession;
import com.dongbang.attendance.domain.AttendanceSessionRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AttendanceSessionJpaRepository
        extends JpaRepository<AttendanceSession, Long>, AttendanceSessionRepository {
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AttendanceSession s where s.eventId = :eventId")
    Optional<AttendanceSession> findForUpdateByEventId(@Param("eventId") Long eventId);
}
