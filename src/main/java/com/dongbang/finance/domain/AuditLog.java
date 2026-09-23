package com.dongbang.finance.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id") private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @Column(name = "actor_membership_id", nullable = false) private Long actorMembershipId;
    @Column(nullable = false, length = 50) private String action;
    @Column(name = "entity_type", nullable = false, length = 50) private String entityType;
    @Column(name = "entity_id", nullable = false) private Long entityId;
    @Column(name = "before_snapshot", columnDefinition = "TEXT") private String beforeSnapshot;
    @Column(name = "after_snapshot", columnDefinition = "TEXT") private String afterSnapshot;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public AuditLog(Long organizationId, Long actorId, String action, String entityType, Long entityId,
                    String beforeSnapshot, String afterSnapshot) {
        this.organizationId = organizationId; this.actorMembershipId = actorId; this.action = action;
        this.entityType = entityType; this.entityId = entityId;
        this.beforeSnapshot = beforeSnapshot; this.afterSnapshot = afterSnapshot;
    }
}
