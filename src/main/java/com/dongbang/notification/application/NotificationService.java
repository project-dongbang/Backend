package com.dongbang.notification.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.notification.domain.Notification;
import com.dongbang.notification.domain.repository.NotificationRepository;
import com.dongbang.notification.exception.NotificationErrorCode;
import com.dongbang.notification.presentation.dto.NotificationDtos.NotificationItem;
import com.dongbang.notification.presentation.dto.NotificationDtos.NotificationListResult;
import com.dongbang.notification.presentation.dto.NotificationDtos.NotificationReadAllResult;
import com.dongbang.notification.presentation.dto.NotificationDtos.NotificationReadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationAccessService accessService;
    private final Clock clock;

    public NotificationListResult getNotifications(Long organizationId, Long userId, boolean unreadOnly,
                                                   String cursor, int size) {
        accessService.requireMember(organizationId, userId);
        NotificationCursor decodedCursor = decodeCursor(cursor);
        PageRequest limit = PageRequest.of(0, size + 1);
        List<Notification> fetched = decodedCursor == null
                ? notificationRepository.findFirstSlice(organizationId, userId, unreadOnly, limit)
                : notificationRepository.findSliceAfter(organizationId, userId, unreadOnly,
                        decodedCursor.createdAt(), decodedCursor.notificationId(), limit);

        boolean hasNext = fetched.size() > size;
        List<Notification> page = hasNext ? fetched.subList(0, size) : fetched;
        List<NotificationItem> content = page.stream().map(this::toItem).toList();
        String nextCursor = hasNext ? encodeCursor(page.getLast()) : null;
        long unreadCount = notificationRepository.countByOrganizationIdAndUserIdAndReadAtIsNull(
                organizationId, userId);
        return new NotificationListResult(unreadCount, content, size, nextCursor, hasNext);
    }

    @Transactional
    public NotificationReadResult markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new GeneralException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getUserId().equals(userId)) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }
        notification.markRead(clock.instant());
        return new NotificationReadResult(notification.getId(), true, notification.getReadAt());
    }

    @Transactional
    public NotificationReadAllResult markAllRead(Long organizationId, Long userId) {
        accessService.requireMember(organizationId, userId);
        int updatedCount = notificationRepository.markAllRead(organizationId, userId, clock.instant());
        long unreadCount = notificationRepository.countByOrganizationIdAndUserIdAndReadAtIsNull(
                organizationId, userId);
        return new NotificationReadAllResult(updatedCount, unreadCount);
    }

    private NotificationItem toItem(Notification notification) {
        return new NotificationItem(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }

    private String encodeCursor(Notification notification) {
        String value = notification.getCreatedAt() + "|" + notification.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private NotificationCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int separator = decoded.lastIndexOf('|');
            if (separator <= 0 || separator == decoded.length() - 1) {
                throw new IllegalArgumentException("Invalid cursor");
            }
            return new NotificationCursor(
                    Instant.parse(decoded.substring(0, separator)),
                    Long.parseLong(decoded.substring(separator + 1))
            );
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        }
    }

    private record NotificationCursor(Instant createdAt, Long notificationId) {
    }
}
