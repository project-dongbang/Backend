package com.dongbang.notification.application;

import com.dongbang.attendance.application.event.AttendanceStartedEvent;
import com.dongbang.event.application.event.ScheduleCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationDomainEventListener {

    private final EventNotificationService eventNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onScheduleCreated(ScheduleCreatedEvent event) {
        eventNotificationService.createScheduleCreated(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAttendanceStarted(AttendanceStartedEvent event) {
        eventNotificationService.createAttendanceStarted(event);
    }
}
