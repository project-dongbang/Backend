package com.dongbang.auth.application;

import com.dongbang.auth.application.token.TokenPair;

import java.net.URI;

public record OAuthLoginResult(URI redirectUri, TokenPair tokens, boolean onboardingRequired) {
}
