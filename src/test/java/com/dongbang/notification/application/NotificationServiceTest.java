package com.dongbang.notification.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.notification.domain.Notification;
import com.dongbang.notification.domain.NotificationReferenceType;
import com.dongbang.notification.domain.NotificationType;
import com.dongbang.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationAccessService accessService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-24T01:00:00Z"), ZoneOffset.UTC);
        notificationService = new NotificationService(notificationRepository, accessService, clock);
    }

    @Test
    void marksOwnedNotificationAsRead() {
        Notification notification = notification(10L);
        given(notificationRepository.findById(501L)).willReturn(Optional.of(notification));

        var result = notificationService.markRead(501L, 10L);

        assertThat(result.notificationId()).isEqualTo(501L);
        assertThat(result.isRead()).isTrue();
        assertThat(result.readAt()).isEqualTo(Instant.parse("2026-09-24T01:00:00Z"));
    }

    @Test
    void rejectsReadingAnotherUsersNotification() {
        given(notificationRepository.findById(501L)).willReturn(Optional.of(notification(10L)));

        assertThatThrownBy(() -> notificationService.markRead(501L, 99L))
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> assertThat(((GeneralException) exception).getErrorCode().getCode())
                        .isEqualTo("AUTH_403_001"));
    }

    @Test
    void rejectsMalformedCursor() {
        assertThatThrownBy(() -> notificationService.getNotifications(1L, 10L, false, "invalid", 20))
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> assertThat(((GeneralException) exception).getErrorCode().getCode())
                        .isEqualTo("COMMON_400_002"));
    }

    private Notification notification(Long userId) {
        Notification notification = Notification.create(
                1L,
                userId,
                NotificationType.FEE_DUE_REMINDER,
                "납부 마감 하루 전입니다.",
                "납부할 금액은 40,000원입니다.",
                NotificationReferenceType.FEE_ITEM,
                21L,
                "FEE_DUE_REMINDER:21:2026-09-25:" + userId
        );
        ReflectionTestUtils.setField(notification, "id", 501L);
        return notification;
    }
}
