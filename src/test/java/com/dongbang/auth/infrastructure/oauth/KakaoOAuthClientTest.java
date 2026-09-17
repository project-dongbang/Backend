package com.dongbang.auth.infrastructure.oauth;

import com.dongbang.auth.application.oauth.OAuthProfile;
import com.dongbang.auth.domain.OAuthProvider;
import com.dongbang.auth.infrastructure.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoOAuthClientTest {

    private static final String CALLBACK_URI = "http://localhost:8080/api/v1/auth/oauth/kakao/callback";

    private MockRestServiceServer server;
    private KakaoOAuthClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new KakaoOAuthClient(authProperties("account_email"), restClientBuilder);
    }

    @Test
    @DisplayName("Kakao 인가 URL에 REST API 키, 콜백, 이메일 범위와 state를 포함한다")
    void createAuthorizationUri() {
        var uri = client.createAuthorizationUri("signed-state");
        var query = UriComponentsBuilder.fromUri(uri).build().getQueryParams();

        assertThat(uri.getScheme()).isEqualTo("https");
        assertThat(uri.getHost()).isEqualTo("kauth.kakao.com");
        assertThat(uri.getPath()).isEqualTo("/oauth/authorize");
        assertThat(query.getFirst("client_id")).isEqualTo("rest-api-key");
        assertThat(query.getFirst("redirect_uri")).isEqualTo(CALLBACK_URI);
        assertThat(query.getFirst("response_type")).isEqualTo("code");
        assertThat(query.getFirst("scope")).isEqualTo("account_email");
        assertThat(query.getFirst("state")).isEqualTo("signed-state");
    }

    @Test
    @DisplayName("Kakao 이메일 권한을 설정하지 않으면 인가 URL에서 scope를 생략한다")
    void createAuthorizationUriWithoutOptionalScope() {
        KakaoOAuthClient clientWithoutScope = new KakaoOAuthClient(
                authProperties(""),
                RestClient.builder()
        );

        var uri = clientWithoutScope.createAuthorizationUri("signed-state");
        var query = UriComponentsBuilder.fromUri(uri).build().getQueryParams();

        assertThat(query).doesNotContainKey("scope");
    }

    @Test
    @DisplayName("Kakao 인가 코드를 토큰으로 교환하고 인증된 회원번호와 이메일을 조회한다")
    void fetchProfile() {
        server.expect(once(), requestTo("https://kauth.kakao.com/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    assertThat(request.getHeaders().getContentType())
                            .isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
                    assertThat(request).isInstanceOf(MockClientHttpRequest.class);
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    assertThat(body)
                            .contains("grant_type=authorization_code")
                            .contains("client_id=rest-api-key")
                            .contains("client_secret=client-secret")
                            .contains("code=authorization-code")
                            .contains("redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fapi%2Fv1%2Fauth%2Foauth%2Fkakao%2Fcallback");
                })
                .andRespond(withSuccess("""
                        {
                          "token_type": "bearer",
                          "access_token": "kakao-access-token",
                          "expires_in": 43199
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), request -> {
                    assertThat(request.getURI().getScheme()).isEqualTo("https");
                    assertThat(request.getURI().getHost()).isEqualTo("kapi.kakao.com");
                    assertThat(request.getURI().getPath()).isEqualTo("/v2/user/me");
                    assertThat(request.getURI().getRawQuery())
                            .isEqualTo("property_keys=%5B%22kakao_account.email%22%5D");
                })
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                        .isEqualTo("Bearer kakao-access-token"))
                .andRespond(withSuccess("""
                        {
                          "id": 123456789,
                          "kakao_account": {
                            "is_email_valid": true,
                            "is_email_verified": true,
                            "email": "oauth@kakao.example"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        OAuthProfile profile = client.fetchProfile("authorization-code");

        assertThat(profile.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(profile.providerUserId()).isEqualTo("123456789");
        assertThat(profile.providerEmail()).isEqualTo("oauth@kakao.example");
        server.verify();
    }

    @Test
    @DisplayName("Kakao 이메일이 유효하거나 인증되지 않았다면 회원번호만 사용한다")
    void fetchProfileWithoutUsableEmail() {
        server.expect(once(), requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withSuccess("{\"access_token\":\"kakao-access-token\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/user/me"))
                .andRespond(withSuccess("""
                        {
                          "id": 123456789,
                          "kakao_account": {
                            "is_email_valid": true,
                            "is_email_verified": false,
                            "email": "unverified@kakao.example"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        OAuthProfile profile = client.fetchProfile("authorization-code");

        assertThat(profile.providerUserId()).isEqualTo("123456789");
        assertThat(profile.providerEmail()).isNull();
        server.verify();
    }

    private AuthProperties authProperties(String scope) {
        AuthProperties.Provider kakao = new AuthProperties.Provider(
                "rest-api-key",
                "client-secret",
                CALLBACK_URI,
                "https://kauth.kakao.com/oauth/authorize",
                "https://kauth.kakao.com/oauth/token",
                "https://kapi.kakao.com/v2/user/me",
                scope
        );
        return new AuthProperties(
                "dongbang-test",
                "dongbang-test-jwt-secret-key-over-32-bytes",
                Duration.ofMinutes(15),
                Duration.ofDays(14),
                Duration.ofMinutes(5),
                false,
                List.of("http://localhost:5173"),
                null,
                kakao
        );
    }
}
