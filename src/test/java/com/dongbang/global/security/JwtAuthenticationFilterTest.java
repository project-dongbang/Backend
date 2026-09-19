package com.dongbang.global.security;

import com.dongbang.auth.application.token.AccessTokenClaims;
import com.dongbang.auth.exception.AuthErrorCode;
import com.dongbang.auth.infrastructure.token.JwtTokenService;
import com.dongbang.auth.infrastructure.web.AuthCookieService;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.user.application.facade.UserAccountFacade;
import com.dongbang.user.application.facade.UserAccountSummary;
import com.dongbang.user.domain.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private UserAccountFacade userAccountFacade;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("성공: 쿠키에 access_token이 있으면 인증이 설정된다")
    void authenticate_viaCookie() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthCookieService.ACCESS_TOKEN_COOKIE, "valid-cookie-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenService.parseAccessToken("valid-cookie-token"))
                .willReturn(new AccessTokenClaims(1L, Instant.now().plusSeconds(900)));
        given(userAccountFacade.getAccount(1L))
                .willReturn(new UserAccountSummary(1L, "홍길동", "20240001", "컴퓨터공학과", "test@dongbang.com", UserStatus.ACTIVE, Instant.now()));

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(1L);
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("성공: Authorization Bearer 헤더가 있으면 인증이 설정된다")
    void authenticate_viaBearerHeader() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-bearer-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenService.parseAccessToken("valid-bearer-token"))
                .willReturn(new AccessTokenClaims(2L, Instant.now().plusSeconds(900)));
        given(userAccountFacade.getAccount(2L))
                .willReturn(new UserAccountSummary(2L, "이순신", "20240002", "기계공학과", "test2@dongbang.com", UserStatus.ACTIVE, Instant.now()));

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(2L);
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("인증 정보가 없을 때 필터는 다음 체인으로 넘어가고 인증 객체는 null이다")
    void noAuthentication_continues() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰이 유효하지 않을 때 인증 컨텍스트를 비우고 체인을 진행한다")
    void invalidToken_clearsContextAndContinues() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenService.parseAccessToken("invalid-token"))
                .willThrow(new GeneralException(com.dongbang.global.response.code.GeneralErrorCode.UNAUTHORIZED));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
