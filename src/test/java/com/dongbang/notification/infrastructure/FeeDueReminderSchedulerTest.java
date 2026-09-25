package com.dongbang.notification.infrastructure;

import com.dongbang.notification.application.FeeDueReminderService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FeeDueReminderSchedulerTest {

    @Test
    void schedulesReminderForTomorrowInSeoul() {
        FeeDueReminderService reminderService = mock(FeeDueReminderService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-24T00:00:00Z"), ZoneOffset.UTC);
        FeeDueReminderScheduler scheduler = new FeeDueReminderScheduler(reminderService, clock);

        scheduler.createTomorrowReminders();

        verify(reminderService).createRemindersFor(java.time.LocalDate.of(2026, 9, 25));
    }
}
