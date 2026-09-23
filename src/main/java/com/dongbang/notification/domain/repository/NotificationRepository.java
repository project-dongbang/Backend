package com.dongbang.notification.domain.repository;

import com.dongbang.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            WHERE n.organizationId = :organizationId
              AND n.userId = :userId
              AND (:unreadOnly = false OR n.readAt IS NULL)
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<Notification> findFirstSlice(
            @Param("organizationId") Long organizationId,
            @Param("userId") Long userId,
            @Param("unreadOnly") boolean unreadOnly,
            Pageable pageable
    );

    @Query("""
            SELECT n FROM Notification n
            WHERE n.organizationId = :organizationId
              AND n.userId = :userId
              AND (:unreadOnly = false OR n.readAt IS NULL)
              AND (n.createdAt < :cursorCreatedAt
                   OR (n.createdAt = :cursorCreatedAt AND n.id < :cursorId))
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<Notification> findSliceAfter(
            @Param("organizationId") Long organizationId,
            @Param("userId") Long userId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    long countByOrganizationIdAndUserIdAndReadAtIsNull(Long organizationId, Long userId);

    List<Notification> findAllByDeduplicationKeyIn(Collection<String> deduplicationKeys);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notification n
            SET n.readAt = :readAt
            WHERE n.organizationId = :organizationId
              AND n.userId = :userId
              AND n.readAt IS NULL
            """)
    int markAllRead(
            @Param("organizationId") Long organizationId,
            @Param("userId") Long userId,
            @Param("readAt") Instant readAt
    );
}
