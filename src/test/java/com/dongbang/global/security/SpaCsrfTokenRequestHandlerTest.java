package com.dongbang.global.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import static org.assertj.core.api.Assertions.assertThat;

class SpaCsrfTokenRequestHandlerTest {

    @Test
    @DisplayName("SPA가 쿠키에서 읽은 일반 CSRF 토큰을 헤더로 보내면 그대로 검증한다")
    void resolvesPlainCsrfTokenFromHeader() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-XSRF-TOKEN", "raw-csrf-token");
        var expectedToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "raw-csrf-token");

        String actualToken = new SpaCsrfTokenRequestHandler()
                .resolveCsrfTokenValue(request, expectedToken);

        assertThat(actualToken).isEqualTo("raw-csrf-token");
    }
}
