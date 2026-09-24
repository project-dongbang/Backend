package com.dongbang.notification.infrastructure;

import com.dongbang.notification.application.EventNotificationService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventDayBeforeReminderSchedulerTest {
    @Test
    void schedulesTomorrowInSeoul() {
        EventNotificationService service = mock(EventNotificationService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-24T15:30:00Z"), ZoneOffset.UTC);

        new EventDayBeforeReminderScheduler(service, clock).createTomorrowReminders();

        verify(service).createEventDayBeforeReminders(LocalDate.of(2026, 9, 26));
    }
}
