package com.dongbang.organization.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "memberships")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "member_name", nullable = false, length = 100)
    private String memberName;

    @Column(name = "student_number", nullable = false, length = 20)
    private String studentNumber;

    @Column(name = "generation", length = 20)
    private String generation;

    @Column(name = "position", length = 50)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MembershipRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MembershipStatus status;

    @CreatedDate
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "linked_at")
    private Instant linkedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Builder
    public Membership(Long id, Organization organization, Long userId, String memberName,
                      String studentNumber, String generation, String position,
                      MembershipRole role) {
        this.id = id;
        this.organization = organization;
        this.userId = userId;
        this.memberName = memberName;
        this.studentNumber = studentNumber;
        this.generation = generation;
        this.position = position;
        this.role = role != null ? role : MembershipRole.MEMBER;
        this.status = MembershipStatus.ACTIVE;
        this.joinedAt = Instant.now();
        this.linkedAt = userId != null ? Instant.now() : null;
    }

    public void updateRole(MembershipRole newRole) {
        this.role = newRole;
    }

    public void leave() {
        this.status = MembershipStatus.LEFT;
        this.leftAt = Instant.now();
    }

    public void expel() {
        this.status = MembershipStatus.EXPELLED;
        this.leftAt = Instant.now();
    }
}
