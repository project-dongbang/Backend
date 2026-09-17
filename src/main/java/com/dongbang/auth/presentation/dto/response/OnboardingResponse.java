package com.dongbang.auth.presentation.dto.response;

import com.dongbang.user.application.facade.UserAccountSummary;
import com.dongbang.user.domain.UserStatus;

import java.time.Instant;

public record OnboardingResponse(
        Long userId,
        String name,
        String studentNumber,
        String department,
        String email,
        UserStatus status,
        Instant onboardingCompletedAt
) {
    public static OnboardingResponse from(UserAccountSummary user) {
        return new OnboardingResponse(
                user.userId(),
                user.name(),
                user.studentNumber(),
                user.department(),
                user.email(),
                user.status(),
                user.onboardingCompletedAt()
        );
    }
}
