package com.dongbang.notification.infrastructure;

import com.dongbang.notification.application.EventNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class EventDayBeforeReminderScheduler {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final EventNotificationService eventNotificationService;
    private final Clock clock;

    @Scheduled(cron = "${app.notification.event-day-before-reminder-cron:0 0 9 * * *}", zone = "Asia/Seoul")
    public void createTomorrowReminders() {
        LocalDate tomorrow = LocalDate.now(clock.withZone(SEOUL)).plusDays(1);
        eventNotificationService.createEventDayBeforeReminders(tomorrow);
    }
}
