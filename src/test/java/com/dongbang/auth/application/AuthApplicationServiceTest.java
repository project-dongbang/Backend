package com.dongbang.auth.application;

import com.dongbang.auth.application.oauth.OAuthProfile;
import com.dongbang.auth.application.oauth.OAuthProviderClient;
import com.dongbang.auth.application.oauth.OAuthProviderRegistry;
import com.dongbang.auth.application.token.OAuthStateClaims;
import com.dongbang.auth.application.token.RefreshTokenClaims;
import com.dongbang.auth.application.token.TokenPair;
import com.dongbang.auth.domain.AuthSession;
import com.dongbang.auth.domain.OAuthAccount;
import com.dongbang.auth.domain.OAuthProvider;
import com.dongbang.auth.domain.repository.AuthSessionRepository;
import com.dongbang.auth.domain.repository.OAuthAccountRepository;
import com.dongbang.auth.infrastructure.config.AuthProperties;
import com.dongbang.auth.infrastructure.token.JwtTokenService;
import com.dongbang.auth.infrastructure.token.TokenHashService;
import com.dongbang.auth.presentation.dto.request.OnboardingRequest;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.application.facade.MembershipLinkFacade;
import com.dongbang.user.application.facade.UserAccountFacade;
import com.dongbang.user.application.facade.UserAccountSummary;
import com.dongbang.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock private OAuthProviderRegistry providerRegistry;
    @Mock private OAuthAccountRepository oauthAccountRepository;
    @Mock private AuthSessionRepository authSessionRepository;
    @Mock private UserAccountFacade userAccountFacade;
    @Mock private MembershipLinkFacade membershipLinkFacade;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private TokenHashService tokenHashService;
    @Mock private OAuthProviderClient oauthProviderClient;

    private AuthApplicationService authService;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties(
                "dongbang-test",
                "dongbang-test-jwt-secret-key-over-32-bytes",
                Duration.ofMinutes(15),
                Duration.ofDays(14),
                Duration.ofMinutes(5),
                false,
                List.of("http://localhost:5173/oauth/callback"),
                null,
                null
        );
        authService = new AuthApplicationService(
                providerRegistry,
                oauthAccountRepository,
                authSessionRepository,
                userAccountFacade,
                membershipLinkFacade,
                jwtTokenService,
                tokenHashService,
                properties
        );
    }

    @Test
    @DisplayName("Refresh Token을 한 번 사용하면 DB 해시와 만료 정보를 새 토큰 기준으로 회전한다")
    void refreshRotatesStoredToken() {
        UUID sessionKey = UUID.randomUUID();
        Instant now = Instant.now();
        AuthSession session = AuthSession.create(
                7L,
                sessionKey,
                "old-hash",
                now.plus(Duration.ofDays(1)),
                "old-device",
                "127.0.0.1"
        );
        TokenPair rotated = new TokenPair(
                "new-access",
                now.plus(Duration.ofMinutes(15)),
                "new-refresh",
                now.plus(Duration.ofDays(14)),
                sessionKey
        );
        given(jwtTokenService.parseRefreshToken("old-refresh"))
                .willReturn(new RefreshTokenClaims(7L, sessionKey, now.plus(Duration.ofDays(1))));
        given(authSessionRepository.findBySessionKeyForUpdate(sessionKey)).willReturn(Optional.of(session));
        given(tokenHashService.matches("old-refresh", "old-hash")).willReturn(true);
        given(jwtTokenService.issueTokenPair(7L, sessionKey)).willReturn(rotated);
        given(tokenHashService.hash("new-refresh")).willReturn("new-hash");

        TokenPair result = authService.refresh("old-refresh", "new-device", "10.0.0.1");

        assertThat(result).isEqualTo(rotated);
        assertThat(session.getRefreshTokenHash()).isEqualTo("new-hash");
        assertThat(session.getExpiresAt()).isEqualTo(rotated.refreshTokenExpiresAt());
        assertThat(session.getDeviceInfo()).isEqualTo("new-device");
        assertThat(session.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(session.getLastUsedAt()).isNotNull();
        verify(userAccountFacade).getAccount(7L);
    }

    @Test
    @DisplayName("Refresh Token 쿠키가 없으면 공통 미인증 오류를 반환한다")
    void refreshRequiresCookie() {
        assertThatThrownBy(() -> authService.refresh(null, null, null))
                .isInstanceOfSatisfying(GeneralException.class,
                        ex -> assertThat(ex.getErrorCode().getCode()).isEqualTo("AUTH_401_001"));
    }

    @Test
    @DisplayName("신규 Kakao 계정 로그인 시 가입 대기 사용자·소셜 계정·인증 세션을 생성한다")
    void completeOAuthLoginCreatesPendingAccount() {
        String redirectUri = "http://localhost:5173/oauth/callback";
        given(jwtTokenService.parseOAuthState("signed-state"))
                .willReturn(new OAuthStateClaims(OAuthProvider.KAKAO, redirectUri));
        given(providerRegistry.get(OAuthProvider.KAKAO)).willReturn(oauthProviderClient);
        given(oauthProviderClient.fetchProfile("authorization-code"))
                .willReturn(new OAuthProfile(OAuthProvider.KAKAO, "123456789", "oauth@kakao.example"));
        given(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.KAKAO, "123456789"))
                .willReturn(Optional.empty());
        given(userAccountFacade.createPendingUser()).willReturn(7L);
        given(oauthAccountRepository.save(org.mockito.ArgumentMatchers.any(OAuthAccount.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(userAccountFacade.getAccount(7L)).willReturn(new UserAccountSummary(
                7L, null, null, null, null, UserStatus.PENDING_ONBOARDING, null
        ));
        given(jwtTokenService.issueTokenPair(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any(UUID.class)
        )).willAnswer(invocation -> {
            UUID sessionKey = invocation.getArgument(1);
            Instant now = Instant.now();
            return new TokenPair(
                    "access-token",
                    now.plus(Duration.ofMinutes(15)),
                    "refresh-token",
                    now.plus(Duration.ofDays(14)),
                    sessionKey
            );
        });
        given(tokenHashService.hash("refresh-token")).willReturn("refresh-hash");
        given(authSessionRepository.save(org.mockito.ArgumentMatchers.any(AuthSession.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        OAuthLoginResult result = authService.completeOAuthLogin(
                OAuthProvider.KAKAO,
                "authorization-code",
                "signed-state",
                "signed-state",
                null,
                "test-device",
                "127.0.0.1"
        );

        assertThat(result.redirectUri()).isEqualTo(URI.create(redirectUri));
        assertThat(result.onboardingRequired()).isTrue();
        assertThat(result.tokens().accessToken()).isEqualTo("access-token");
        verify(oauthAccountRepository).save(org.mockito.ArgumentMatchers.argThat(account ->
                account.getUserId().equals(7L)
                        && account.getProvider() == OAuthProvider.KAKAO
                        && account.getProviderUserId().equals("123456789")
                        && account.getProviderEmail().equals("oauth@kakao.example")
        ));
        verify(oauthAccountRepository).flush();
        verify(authSessionRepository).save(org.mockito.ArgumentMatchers.argThat(session ->
                session.getUserId().equals(7L)
                        && session.getRefreshTokenHash().equals("refresh-hash")
                        && session.getDeviceInfo().equals("test-device")
        ));
    }

    @Test
    @DisplayName("최초가입 정보를 저장한 뒤 같은 이름과 학번의 미연결 멤버를 사용자 계정에 연결한다")
    void onboardingLinksExistingMemberships() {
        Instant completedAt = Instant.now();
        OnboardingRequest request = new OnboardingRequest(
                "김동방",
                "20260001",
                "컴퓨터공학과",
                "member@example.com"
        );
        UserAccountSummary user = new UserAccountSummary(
                7L,
                request.name(),
                request.studentNumber(),
                request.department(),
                request.email(),
                UserStatus.ACTIVE,
                completedAt
        );
        given(userAccountFacade.completeOnboarding(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(request.name()),
                org.mockito.ArgumentMatchers.eq(request.studentNumber()),
                org.mockito.ArgumentMatchers.eq(request.department()),
                org.mockito.ArgumentMatchers.eq(request.email()),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).willReturn(user);

        var result = authService.completeOnboarding(7L, request);

        assertThat(result.email()).isEqualTo("member@example.com");
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
        verify(membershipLinkFacade).linkExistingMemberships(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq("김동방"),
                org.mockito.ArgumentMatchers.eq("20260001"),
                org.mockito.ArgumentMatchers.any(Instant.class)
        );
    }
}
