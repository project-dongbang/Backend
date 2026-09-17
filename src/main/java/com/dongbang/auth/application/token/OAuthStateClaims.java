package com.dongbang.auth.application.token;

import com.dongbang.auth.domain.OAuthProvider;

public record OAuthStateClaims(OAuthProvider provider, String redirectUri) {
}
