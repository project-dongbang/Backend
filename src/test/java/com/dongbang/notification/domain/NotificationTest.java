package com.dongbang.notification.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    @Test
    void markReadIsIdempotent() {
        Notification notification = Notification.create(
                1L,
                2L,
                NotificationType.FEE_DUE_REMINDER,
                "납부 마감 하루 전입니다.",
                "납부할 금액은 40,000원입니다.",
                NotificationReferenceType.FEE_ITEM,
                3L,
                "FEE_DUE_REMINDER:3:2026-09-25:2"
        );
        Instant firstReadAt = Instant.parse("2026-09-24T01:00:00Z");

        notification.markRead(firstReadAt);
        notification.markRead(firstReadAt.plusSeconds(60));

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
    }
}
