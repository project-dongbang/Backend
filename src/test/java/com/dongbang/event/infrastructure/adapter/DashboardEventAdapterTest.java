package com.dongbang.event.infrastructure.adapter;

import com.dongbang.event.domain.Event;
import com.dongbang.event.domain.EventType;
import com.dongbang.event.infrastructure.persistence.EventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DashboardEventAdapterTest {
    private final EventJpaRepository events = mock(EventJpaRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneOffset.UTC);
    private final DashboardEventAdapter adapter = new DashboardEventAdapter(events, clock);

    @Test
    void usesSeoulMonthBoundariesAndFormatsLimitedUpcomingSchedules() {
        given(events.countDashboardEvents(1L, Instant.parse("2026-09-30T15:00:00Z"),
                Instant.parse("2026-10-31T15:00:00Z"))).willReturn(3L);
        Event schedule = mock(Event.class);
        given(schedule.getId()).willReturn(4L);
        given(schedule.getStartsAt()).willReturn(Instant.parse("2026-10-09T10:00:00Z"));
        given(schedule.getTitle()).willReturn("정기 모임");
        given(schedule.getLocation()).willReturn("동아리방");
        given(schedule.getType()).willReturn(EventType.SCHEDULE);
        given(events.findDashboardUpcoming(1L, clock.instant(), PageRequest.of(0, 5)))
                .willReturn(List.of(schedule));

        assertThat(adapter.countThisMonthEvents(1L)).isEqualTo(3);
        var upcoming = adapter.getUpcomingSchedules(1L, 5);
        assertThat(upcoming).hasSize(1);
        assertThat(upcoming.getFirst().day()).isEqualTo("09");
        assertThat(upcoming.getFirst().month()).isEqualTo(10);
        assertThat(upcoming.getFirst().detail()).isEqualTo("19:00 · 동아리방");
        assertThat(upcoming.getFirst().status()).isEqualTo("일정");
        verify(events).findDashboardUpcoming(1L, clock.instant(), PageRequest.of(0, 5));
    }
}
