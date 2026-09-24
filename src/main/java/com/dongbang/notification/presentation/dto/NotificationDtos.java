package com.dongbang.notification.presentation.dto;

import com.dongbang.notification.domain.NotificationReferenceType;
import com.dongbang.notification.domain.NotificationType;

import java.time.Instant;
import java.util.List;

public final class NotificationDtos {
    private NotificationDtos() {
    }

    public record NotificationListResult(
            long unreadCount,
            List<NotificationItem> content,
            int size,
            String nextCursor,
            boolean hasNext
    ) {
    }

    public record NotificationItem(
            Long notificationId,
            NotificationType notificationType,
            String title,
            String message,
            NotificationReferenceType referenceType,
            Long referenceId,
            boolean isRead,
            Instant readAt,
            Instant sentAt
    ) {
    }

    public record NotificationReadResult(Long notificationId, boolean isRead, Instant readAt) {
    }

    public record NotificationReadAllResult(int updatedCount, long unreadCount) {
    }
}
