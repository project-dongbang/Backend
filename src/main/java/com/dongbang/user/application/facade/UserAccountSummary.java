package com.dongbang.user.application.facade;

import com.dongbang.user.domain.UserStatus;

import java.time.Instant;

public record UserAccountSummary(
        Long userId,
        String name,
        String studentNumber,
        String department,
        String email,
        UserStatus status,
        Instant onboardingCompletedAt
) {
    public boolean onboardingRequired() {
        return status == UserStatus.PENDING_ONBOARDING;
    }
}
