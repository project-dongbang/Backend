package com.dongbang.auth.presentation.dto.response;

import com.dongbang.auth.domain.OAuthProvider;
import com.dongbang.user.application.facade.UserAccountSummary;
import com.dongbang.user.domain.UserStatus;

import java.util.List;

public record AuthMeResponse(
        Long userId,
        String name,
        String email,
        UserStatus status,
        boolean onboardingRequired,
        List<OAuthProvider> oauthProviders
) {
    public static AuthMeResponse of(UserAccountSummary user, List<OAuthProvider> providers) {
        return new AuthMeResponse(
                user.userId(),
                user.name(),
                user.email(),
                user.status(),
                user.onboardingRequired(),
                providers
        );
    }
}
