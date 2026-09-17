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
public class KakaoOAuthClient implements OAuthProviderClient {

    private static final String EMAIL_PROPERTY_KEYS = "[\"kakao_account.email\"]";

    private final AuthProperties.Provider properties;
    private final RestClient restClient;

    public KakaoOAuthClient(AuthProperties authProperties, RestClient.Builder restClientBuilder) {
        this.properties = authProperties.kakao();
        this.restClient = restClientBuilder.build();
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public URI createAuthorizationUri(String state) {
        validateConfigured();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.authorizationUri())
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.callbackUri())
                .queryParam("response_type", "code")
                .queryParam("state", state);
        if (properties.scope() != null && !properties.scope().isBlank()) {
            builder.queryParam("scope", properties.scope());
        }
        return builder.build().encode().toUri();
    }

    @Override
    public OAuthProfile fetchProfile(String authorizationCode) {
        validateConfigured();
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "authorization_code");
            form.add("client_id", properties.clientId());
            form.add("redirect_uri", properties.callbackUri());
            form.add("code", authorizationCode);
            if (properties.clientSecret() != null && !properties.clientSecret().isBlank()) {
                form.add("client_secret", properties.clientSecret());
            }

            KakaoTokenResponse token = restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
            if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
                throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
            }

            URI userInfoUri = createUserInfoUri();
            KakaoUserInfo user = restClient.get()
                    .uri(userInfoUri)
                    .headers(headers -> headers.setBearerAuth(token.accessToken()))
                    .retrieve()
                    .body(KakaoUserInfo.class);
            if (user == null || user.id() == null) {
                throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
            }
            String email = user.kakaoAccount() != null && user.kakaoAccount().hasUsableEmail()
                    ? user.kakaoAccount().email()
                    : null;
            return new OAuthProfile(provider(), user.id().toString(), email);
        } catch (GeneralException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
    }

    private void validateConfigured() {
        if (!properties.isConfigured()) {
            throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
    }

    private URI createUserInfoUri() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.userInfoUri());
        if (requestsEmailScope()) {
            builder.queryParam("property_keys", EMAIL_PROPERTY_KEYS);
        }
        return builder.build().encode().toUri();
    }

    private boolean requestsEmailScope() {
        if (properties.scope() == null || properties.scope().isBlank()) {
            return false;
        }
        return properties.scope().lines()
                .flatMap(line -> java.util.Arrays.stream(line.split("[,\\s]+")))
                .anyMatch("account_email"::equals);
    }

    private record KakaoTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {
    }

    private record KakaoUserInfo(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {
    }

    private record KakaoAccount(
            @JsonProperty("is_email_valid") Boolean emailValid,
            @JsonProperty("is_email_verified") Boolean emailVerified,
            String email
    ) {
        private boolean hasUsableEmail() {
            return Boolean.TRUE.equals(emailValid)
                    && Boolean.TRUE.equals(emailVerified)
                    && email != null
                    && !email.isBlank();
        }
    }
}
