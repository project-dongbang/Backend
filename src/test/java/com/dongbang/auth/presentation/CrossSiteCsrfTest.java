package com.dongbang.auth.presentation;

import com.dongbang.auth.application.AuthApplicationService;
import com.dongbang.auth.infrastructure.config.AuthProperties;
import com.dongbang.auth.infrastructure.token.JwtTokenService;
import com.dongbang.auth.infrastructure.web.AuthCookieService;
import com.dongbang.global.config.SecurityConfig;
import com.dongbang.global.config.WebMvcConfig;
import com.dongbang.global.security.ApiSecurityExceptionHandler;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, ApiSecurityExceptionHandler.class, WebMvcConfig.class,
        CrossSiteCsrfTest.ProductionAuthProperties.class})
class CrossSiteCsrfTest {

    private static final String FRONTEND = "https://dongbang-frontend.vercel.app";

    @TestConfiguration
    static class ProductionAuthProperties {
        @Bean
        AuthProperties authProperties() {
            return new AuthProperties("dongbang", "test-secret", Duration.ofMinutes(15),
                    Duration.ofDays(14), Duration.ofMinutes(5), true,
                    List.of(FRONTEND + "/auth/callback"), null, null);
        }
    }

    @Autowired private MockMvc mvc;
    @MockitoBean private AuthApplicationService authService;
    @MockitoBean private AuthCookieService cookieService;
    @MockitoBean private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("교차 사이트 프론트에서 원본 CSRF 토큰을 받아 변경 요청에 사용할 수 있다")
    void csrfTokenFlow() throws Exception {
        MvcResult csrfResult = mvc.perform(get("/api/v1/auth/csrf")
                        .header("Origin", FRONTEND))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(jsonPath("$.result.headerName").value("X-XSRF-TOKEN"))
                .andReturn();

        String token = JsonPath.read(csrfResult.getResponse().getContentAsString(), "$.result.token");
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        assertThat(token).isEqualTo(csrfCookie.getValue());
        assertThat(csrfCookie.getSecure()).isTrue();
        assertThat(csrfCookie.getAttribute("SameSite")).isEqualTo("None");
        assertThat(csrfCookie.getPath()).isEqualTo("/");

        String onboarding = """
                {"name":"김동방","studentNumber":"20260001",
                 "department":"컴퓨터공학과","email":"member@example.com"}
                """;
        mvc.perform(put("/api/v1/auth/onboarding")
                        .with(user("7").roles("ONBOARDING"))
                        .header("Origin", FRONTEND)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onboarding))
                .andExpect(status().isOk());

        mvc.perform(put("/api/v1/auth/onboarding")
                        .with(user("7").roles("ONBOARDING"))
                        .header("Origin", FRONTEND)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onboarding))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/auth/onboarding")
                        .with(user("7").roles("ONBOARDING"))
                        .header("Origin", FRONTEND)
                        .cookie(csrfCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onboarding))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/auth/csrf")
                        .header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden());
    }
}
