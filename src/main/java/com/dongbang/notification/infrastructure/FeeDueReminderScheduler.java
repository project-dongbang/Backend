package com.dongbang.notification.infrastructure;

import com.dongbang.notification.application.FeeDueReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class FeeDueReminderScheduler {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final FeeDueReminderService reminderService;
    private final Clock clock;

    @Scheduled(cron = "${app.notification.fee-due-reminder-cron:0 0 9 * * *}", zone = "Asia/Seoul")
    public void createTomorrowReminders() {
        LocalDate tomorrow = LocalDate.now(clock.withZone(SEOUL)).plusDays(1);
        reminderService.createRemindersFor(tomorrow);
    }
}
