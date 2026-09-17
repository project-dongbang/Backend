package com.dongbang.global.security;

import com.dongbang.auth.infrastructure.token.JwtTokenService;
import com.dongbang.auth.infrastructure.web.AuthCookieService;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.user.application.facade.UserAccountFacade;
import com.dongbang.user.application.facade.UserAccountSummary;
import com.dongbang.user.domain.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserAccountFacade userAccountFacade;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String accessToken = findCookie(request, AuthCookieService.ACCESS_TOKEN_COOKIE);
        if (accessToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long userId = jwtTokenService.parseAccessToken(accessToken).userId();
                UserAccountSummary user = userAccountFacade.getAccount(userId);
                String role = user.status() == UserStatus.ACTIVE ? "ROLE_USER" : "ROLE_ONBOARDING";
                var authentication = UsernamePasswordAuthenticationToken.authenticated(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (GeneralException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String findCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
