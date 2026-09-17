package com.dongbang.auth.infrastructure.oauth;

import com.dongbang.auth.application.oauth.OAuthProfile;
import com.dongbang.auth.application.oauth.OAuthProviderClient;
import com.dongbang.auth.domain.OAuthProvider;
import com.dongbang.auth.exception.AuthErrorCode;
import com.dongbang.auth.infrastructure.config.AuthProperties;
import com.dongbang.global.exception.GeneralException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class GoogleOAuthClient implements OAuthProviderClient {

    private final AuthProperties.Provider properties;
    private final RestClient restClient;

    public GoogleOAuthClient(AuthProperties authProperties, RestClient.Builder restClientBuilder) {
        this.properties = authProperties.google();
        this.restClient = restClientBuilder.build();
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public URI createAuthorizationUri(String state) {
        validateConfigured();
        return UriComponentsBuilder.fromUriString(properties.authorizationUri())
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.callbackUri())
                .queryParam("response_type", "code")
                .queryParam("scope", properties.scope())
                .queryParam("state", state)
                .queryParam("include_granted_scopes", "true")
                .build()
                .encode()
                .toUri();
    }

    @Override
    public OAuthProfile fetchProfile(String authorizationCode) {
        validateConfigured();
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "authorization_code");
            form.add("client_id", properties.clientId());
            form.add("client_secret", properties.clientSecret());
            form.add("redirect_uri", properties.callbackUri());
            form.add("code", authorizationCode);

            GoogleTokenResponse token = restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
            if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
                throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
            }

            GoogleUserInfo user = restClient.get()
                    .uri(properties.userInfoUri())
                    .headers(headers -> headers.setBearerAuth(token.accessToken()))
                    .retrieve()
                    .body(GoogleUserInfo.class);
            if (user == null || user.sub() == null || user.sub().isBlank()) {
                throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
            }
            return new OAuthProfile(provider(), user.sub(), user.email());
        } catch (GeneralException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
    }

    private void validateConfigured() {
        if (!properties.isConfigured() || properties.clientSecret() == null || properties.clientSecret().isBlank()) {
            throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
    }

    private record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {
    }

    private record GoogleUserInfo(
            String sub,
            String email
    ) {
    }
}
