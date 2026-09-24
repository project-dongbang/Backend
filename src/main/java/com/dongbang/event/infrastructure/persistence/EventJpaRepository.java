package com.dongbang.event.infrastructure.persistence;

import com.dongbang.event.domain.Event;
import com.dongbang.event.domain.repository.EventRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventJpaRepository extends JpaRepository<Event, Long>, EventRepository {

    // 동시 수정 방지를 위한 행 잠금 (트랜잭션 종료까지 유지)
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id and e.organizationId = :organizationId and e.deletedAt is null")
    Optional<Event> findForUpdate(@Param("id") Long id, @Param("organizationId") Long organizationId);

    // 조회 기간과 겹치는 일정 포함 (이전 달 시작 일정 포함)
    @Override
    @Query("""
            select e from Event e
            where e.organizationId = :organizationId and e.deletedAt is null
              and e.startsAt < :until and e.endsAt > :from
            order by e.startsAt asc, e.id asc
            """)
    List<Event> findOverlapping(@Param("organizationId") Long organizationId,
                                @Param("from") Instant from, @Param("until") Instant until);

    @Override
    @Query("""
            select e from Event e
            where e.organizationId = :organizationId and e.deletedAt is null
              and e.type = com.dongbang.event.domain.EventType.EVENT
            order by e.startsAt desc, e.id desc
            """)
    List<Event> findAttendanceEvents(@Param("organizationId") Long organizationId);
}
