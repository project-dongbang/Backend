package com.dongbang.auth.application.oauth;

import com.dongbang.auth.domain.OAuthProvider;

import java.net.URI;

public interface OAuthProviderClient {
    OAuthProvider provider();
    URI createAuthorizationUri(String state);
    OAuthProfile fetchProfile(String authorizationCode);
}
