package com.dongbang.auth.application;

import java.net.URI;

public record OAuthAuthorizationResult(URI authorizationUri, String stateToken) {
}
