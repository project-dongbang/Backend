package com.dongbang.notification.application;

import com.dongbang.notification.domain.Notification;
import com.dongbang.notification.domain.NotificationReferenceType;
import com.dongbang.notification.domain.NotificationType;
import com.dongbang.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationCreatorTest {
    @Mock NotificationRepository repository;

    @Test
    void savesOnlyNotificationsWhoseDeduplicationKeyDoesNotExist() {
        Notification existing = notification(1L, "key-1");
        Notification fresh = notification(2L, "key-2");
        given(repository.findAllByDeduplicationKeyIn(List.of("key-1", "key-2")))
                .willReturn(List.of(existing));

        int count = new NotificationCreator(repository).saveNew(List.of(existing, fresh));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(count).isEqualTo(1);
        assertThat(captor.getValue()).containsExactly(fresh);
    }

    private Notification notification(Long userId, String key) {
        return Notification.create(1L, userId, NotificationType.SCHEDULE_CREATED,
                "제목", "내용", NotificationReferenceType.EVENT, 10L, key);
    }
}
