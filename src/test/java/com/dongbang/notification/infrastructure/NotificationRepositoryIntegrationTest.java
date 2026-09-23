package com.dongbang.notification.infrastructure;

import com.dongbang.notification.domain.Notification;
import com.dongbang.notification.domain.NotificationReferenceType;
import com.dongbang.notification.domain.NotificationType;
import com.dongbang.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest
@Transactional
class NotificationRepositoryIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private NotificationRepository notificationRepository;

    @Test
    void storesQueriesAndMarksNotificationsAsRead() {
        jdbcTemplate.update("""
                INSERT INTO users (user_id, status, created_at, updated_at)
                VALUES (910001, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO organizations (organization_id, name, slug, status, created_at, updated_at)
                VALUES (920001, '알림 테스트 동아리', 'notification-integration-test', 'ACTIVE',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        Notification first = notificationRepository.save(Notification.create(
                920001L, 910001L, NotificationType.FEE_DUE_REMINDER,
                "첫 번째", "첫 번째 알림", NotificationReferenceType.FEE_ITEM, 1L,
                "integration:first"));
        Notification second = notificationRepository.saveAndFlush(Notification.create(
                920001L, 910001L, NotificationType.FEE_DUE_REMINDER,
                "두 번째", "두 번째 알림", NotificationReferenceType.FEE_ITEM, 2L,
                "integration:second"));

        var notifications = notificationRepository.findFirstSlice(
                920001L, 910001L, false, PageRequest.of(0, 10));

        assertThat(notifications).extracting(Notification::getId)
                .containsExactly(second.getId(), first.getId());
        assertThat(notificationRepository.findSliceAfter(
                920001L, 910001L, false, second.getCreatedAt(), second.getId(), PageRequest.of(0, 10)))
                .extracting(Notification::getId)
                .containsExactly(first.getId());
        assertThat(notificationRepository.countByOrganizationIdAndUserIdAndReadAtIsNull(
                920001L, 910001L)).isEqualTo(2);

        int updatedCount = notificationRepository.markAllRead(
                920001L, 910001L, Instant.parse("2026-09-24T01:00:00Z"));

        assertThat(updatedCount).isEqualTo(2);
        assertThat(notificationRepository.countByOrganizationIdAndUserIdAndReadAtIsNull(
                920001L, 910001L)).isZero();
    }
}
