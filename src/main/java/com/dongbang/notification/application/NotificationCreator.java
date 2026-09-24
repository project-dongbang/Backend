package com.dongbang.notification.application;

import com.dongbang.notification.domain.Notification;
import com.dongbang.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationCreator {

    private final NotificationRepository notificationRepository;

    public int saveNew(List<Notification> candidates) {
        if (candidates.isEmpty()) {
            return 0;
        }

        Set<String> existingKeys = notificationRepository.findAllByDeduplicationKeyIn(
                        candidates.stream().map(Notification::getDeduplicationKey).toList())
                .stream()
                .map(Notification::getDeduplicationKey)
                .collect(Collectors.toSet());
        List<Notification> newNotifications = candidates.stream()
                .filter(notification -> !existingKeys.contains(notification.getDeduplicationKey()))
                .toList();
        if (newNotifications.isEmpty()) {
            return 0;
        }

        notificationRepository.saveAll(newNotifications);
        return newNotifications.size();
    }
}
