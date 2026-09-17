package com.dongbang.auth.application.oauth;

import com.dongbang.auth.domain.OAuthProvider;

public record OAuthProfile(
        OAuthProvider provider,
        String providerUserId,
        String providerEmail
) {
}
