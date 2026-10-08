package com.dongbang.event.infrastructure.adapter;

import com.dongbang.dashboard.application.port.DashboardEventPort;
import com.dongbang.dashboard.application.port.DashboardScheduleItem;
import com.dongbang.event.domain.Event;
import com.dongbang.event.domain.EventType;
import com.dongbang.event.infrastructure.persistence.EventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DashboardEventAdapter implements DashboardEventPort {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final EventJpaRepository events;
    private final Clock clock;

    @Override
    public int countThisMonthEvents(Long organizationId) {
        LocalDate first = LocalDate.now(clock.withZone(SEOUL)).withDayOfMonth(1);
        long count = events.countDashboardEvents(organizationId,
                first.atStartOfDay(SEOUL).toInstant(), first.plusMonths(1).atStartOfDay(SEOUL).toInstant());
        return Math.toIntExact(count);
    }

    @Override
    public List<DashboardScheduleItem> getUpcomingSchedules(Long organizationId, int limit) {
        if (limit <= 0) return List.of();
        return events.findDashboardUpcoming(organizationId, clock.instant(), PageRequest.of(0, limit))
                .stream().map(this::toItem).toList();
    }

    private DashboardScheduleItem toItem(Event event) {
        var start = event.getStartsAt().atZone(SEOUL);
        String detail = start.format(TIME) + " · " + event.getLocation();
        return new DashboardScheduleItem(event.getId(), String.format("%02d", start.getDayOfMonth()),
                start.getMonthValue(), event.getTitle(), detail,
                event.getType() == EventType.EVENT ? "행사" : "일정", event.getStartsAt());
    }
}
