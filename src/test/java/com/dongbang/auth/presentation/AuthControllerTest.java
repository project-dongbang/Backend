package com.dongbang.auth.presentation;

import com.dongbang.auth.application.AuthApplicationService;
import com.dongbang.auth.application.OAuthAuthorizationResult;
import com.dongbang.auth.domain.OAuthProvider;
import com.dongbang.auth.infrastructure.token.JwtTokenService;
import com.dongbang.auth.infrastructure.web.AuthCookieService;
import com.dongbang.auth.presentation.dto.response.AuthMeResponse;
import com.dongbang.auth.presentation.dto.response.OnboardingResponse;
import com.dongbang.global.config.SecurityConfig;
import com.dongbang.global.config.WebMvcConfig;
import com.dongbang.global.security.ApiSecurityExceptionHandler;
import com.dongbang.user.domain.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, ApiSecurityExceptionHandler.class, WebMvcConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean private AuthApplicationService authService;
    @MockitoBean private AuthCookieService cookieService;
    @MockitoBean private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Google OAuth 시작 API는 state 쿠키를 저장하고 Google 인증 화면으로 이동시킨다")
    void authorizeGoogle() throws Exception {
        String redirectUri = "http://localhost:5173";
        URI googleUri = URI.create("https://accounts.google.com/o/oauth2/v2/auth?state=signed-state");
        given(authService.createAuthorization(OAuthProvider.GOOGLE, redirectUri))
                .willReturn(new OAuthAuthorizationResult(googleUri, "signed-state"));
        given(cookieService.oauthState(OAuthProvider.GOOGLE, "signed-state"))
                .willReturn(ResponseCookie.from("oauth_state_google", "signed-state")
                        .httpOnly(true)
                        .path("/api/v1/auth/oauth/google/callback")
                        .build());

        mvc.perform(get("/api/v1/auth/oauth/google/authorize")
                        .queryParam("redirectUri", redirectUri))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", googleUri.toString()))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("oauth_state_google=signed-state")));
    }

    @Test
    @DisplayName("Kakao OAuth 시작 API는 state 쿠키를 저장하고 Kakao 인증 화면으로 이동시킨다")
    void authorizeKakao() throws Exception {
        String redirectUri = "http://localhost:5173";
        URI kakaoUri = URI.create("https://kauth.kakao.com/oauth/authorize?state=signed-state");
        given(authService.createAuthorization(OAuthProvider.KAKAO, redirectUri))
                .willReturn(new OAuthAuthorizationResult(kakaoUri, "signed-state"));
        given(cookieService.oauthState(OAuthProvider.KAKAO, "signed-state"))
                .willReturn(ResponseCookie.from("oauth_state_kakao", "signed-state")
                        .httpOnly(true)
                        .path("/api/v1/auth/oauth/kakao/callback")
                        .build());

        mvc.perform(get("/api/v1/auth/oauth/kakao/authorize")
                        .queryParam("redirectUri", redirectUri))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", kakaoUri.toString()))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("oauth_state_kakao=signed-state")));
    }

    @Test
    @DisplayName("가입 대기 사용자는 내 인증 상태를 조회할 수 있다")
    void getMeBeforeOnboarding() throws Exception {
        given(authService.getMe(7L)).willReturn(new AuthMeResponse(
                7L,
                null,
                null,
                UserStatus.PENDING_ONBOARDING,
                true,
                List.of(OAuthProvider.KAKAO)
        ));

        mvc.perform(get("/api/v1/auth/me")
                        .with(user("7").roles("ONBOARDING")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON_200_001"))
                .andExpect(jsonPath("$.result.userId").value(7))
                .andExpect(jsonPath("$.result.name").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.result.email").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.result.onboardingRequired").value(true))
                .andExpect(jsonPath("$.result.oauthProviders[0]").value("KAKAO"))
                .andExpect(jsonPath("$.errorDetail").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("가입 대기 사용자는 서비스 이메일을 포함한 최초가입 정보를 저장할 수 있다")
    void onboarding() throws Exception {
        given(authService.completeOnboarding(any(), any())).willReturn(new OnboardingResponse(
                7L,
                "김동방",
                "20260001",
                "컴퓨터공학과",
                "member@example.com",
                UserStatus.ACTIVE,
                Instant.parse("2026-09-17T10:00:00Z")
        ));

        mvc.perform(put("/api/v1/auth/onboarding")
                        .with(user("7").roles("ONBOARDING"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "김동방",
                                  "studentNumber": "20260001",
                                  "department": "컴퓨터공학과",
                                  "email": "member@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.userId").value(7))
                .andExpect(jsonPath("$.result.email").value("member@example.com"))
                .andExpect(jsonPath("$.result.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("최초가입 요청에서 서비스 이메일을 누락하면 요청값 검증 오류를 반환한다")
    void onboardingRequiresEmail() throws Exception {
        mvc.perform(put("/api/v1/auth/onboarding")
                        .with(user("7").roles("ONBOARDING"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "김동방",
                                  "studentNumber": "20260001",
                                  "department": "컴퓨터공학과"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_002"))
                .andExpect(jsonPath("$.result").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.errorDetail").isNotEmpty());
    }
}
