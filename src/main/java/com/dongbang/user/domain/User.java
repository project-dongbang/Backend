package com.dongbang.user.domain;

import com.dongbang.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "student_number", length = 20)
    private String studentNumber;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "profile_image_storage_key", length = 500)
    private String profileImageStorageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private User(UserStatus status) {
        this.status = status;
    }

    public static User pendingOnboarding() {
        return new User(UserStatus.PENDING_ONBOARDING);
    }

    public void completeOnboarding(String name, String studentNumber, String department, String email, Instant completedAt) {
        this.name = name;
        this.studentNumber = studentNumber;
        this.department = department;
        this.email = email;
        this.status = UserStatus.ACTIVE;
        this.onboardingCompletedAt = completedAt;
    }

    public void recordLogin(Instant loggedInAt) {
        this.lastLoginAt = loggedInAt;
    }

    public void updateProfile(String name, String studentNumber, String department, String email) {
        if (name != null) {
            this.name = name;
        }
        if (studentNumber != null) {
            this.studentNumber = studentNumber;
        }
        if (department != null) {
            this.department = department;
        }
        if (email != null) {
            this.email = email;
        }
    }

    public boolean requiresOnboarding() {
        return status == UserStatus.PENDING_ONBOARDING;
    }

    public void updateProfileImage(String storageKey) {
        this.profileImageStorageKey = storageKey;
        this.profileImageUrl = null;
    }

    public void useDefaultProfileImage() {
        this.profileImageStorageKey = null;
        this.profileImageUrl = null;
    }
}
