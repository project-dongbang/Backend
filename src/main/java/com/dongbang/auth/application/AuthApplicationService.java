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
import com.dongbang.auth.exception.AuthErrorCode;
import com.dongbang.auth.infrastructure.config.AuthProperties;
import com.dongbang.auth.infrastructure.token.JwtTokenService;
import com.dongbang.auth.infrastructure.token.TokenHashService;
import com.dongbang.auth.presentation.dto.request.OnboardingRequest;
import com.dongbang.auth.presentation.dto.response.AuthMeResponse;
import com.dongbang.auth.presentation.dto.response.OnboardingResponse;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.organization.application.facade.MembershipLinkFacade;
import com.dongbang.user.application.facade.UserAccountFacade;
import com.dongbang.user.application.facade.UserAccountSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthApplicationService {

    private final OAuthProviderRegistry providerRegistry;
    private final OAuthAccountRepository oauthAccountRepository;
    private final AuthSessionRepository authSessionRepository;
    private final UserAccountFacade userAccountFacade;
    private final MembershipLinkFacade membershipLinkFacade;
    private final JwtTokenService jwtTokenService;
    private final TokenHashService tokenHashService;
    private final AuthProperties properties;
    private final Clock clock = Clock.systemUTC();

    @Transactional(readOnly = true)
    public OAuthAuthorizationResult createAuthorization(OAuthProvider provider, String redirectUri) {
        validateRedirectUri(redirectUri);
        String stateToken = jwtTokenService.issueOAuthState(provider, redirectUri);
        URI authorizationUri = providerRegistry.get(provider).createAuthorizationUri(stateToken);
        return new OAuthAuthorizationResult(authorizationUri, stateToken);
    }

    public OAuthLoginResult completeOAuthLogin(
            OAuthProvider provider,
            String authorizationCode,
            String state,
            String stateCookie,
            String oauthError,
            String deviceInfo,
            String ipAddress
    ) {
        if (oauthError != null && !oauthError.isBlank()) {
            throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
        if (authorizationCode == null || authorizationCode.isBlank()
                || state == null || state.isBlank()
                || stateCookie == null || !constantTimeEquals(state, stateCookie)) {
            throw new GeneralException(AuthErrorCode.INVALID_OAUTH_REQUEST);
        }

        OAuthStateClaims stateClaims = jwtTokenService.parseOAuthState(state);
        if (stateClaims.provider() != provider) {
            throw new GeneralException(AuthErrorCode.INVALID_OAUTH_REQUEST);
        }
        validateRedirectUri(stateClaims.redirectUri());

        OAuthProviderClient client = providerRegistry.get(provider);
        OAuthProfile profile = client.fetchProfile(authorizationCode);
        OAuthAccount account = oauthAccountRepository
                .findByProviderAndProviderUserId(provider, profile.providerUserId())
                .orElseGet(() -> createOAuthAccount(profile));
        account.updateProviderEmail(profile.providerEmail());

        Instant now = clock.instant();
        userAccountFacade.recordLogin(account.getUserId(), now);
        UserAccountSummary user = userAccountFacade.getAccount(account.getUserId());
        TokenPair tokens = createSession(account.getUserId(), deviceInfo, ipAddress);
        return new OAuthLoginResult(URI.create(stateClaims.redirectUri()), tokens, user.onboardingRequired());
    }

    public TokenPair refresh(String refreshToken, String deviceInfo, String ipAddress) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
        }
        RefreshTokenClaims claims = jwtTokenService.parseRefreshToken(refreshToken);
        AuthSession session = authSessionRepository.findBySessionKeyForUpdate(claims.sessionKey())
                .orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN));
        Instant now = clock.instant();
        if (!session.getUserId().equals(claims.userId())
                || !session.isActiveAt(now)
                || !tokenHashService.matches(refreshToken, session.getRefreshTokenHash())) {
            throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        userAccountFacade.getAccount(session.getUserId());
        TokenPair rotated = jwtTokenService.issueTokenPair(session.getUserId(), session.getSessionKey());
        session.rotate(
                tokenHashService.hash(rotated.refreshToken()),
                rotated.refreshTokenExpiresAt(),
                now,
                normalizeDeviceInfo(deviceInfo),
                ipAddress
        );
        return rotated;
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
        }
        RefreshTokenClaims claims;
        try {
            claims = jwtTokenService.parseRefreshToken(refreshToken);
        } catch (GeneralException ex) {
            throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
        }
        AuthSession session = authSessionRepository.findBySessionKeyForUpdate(claims.sessionKey())
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.UNAUTHORIZED));
        if (!session.getUserId().equals(claims.userId())
                || !tokenHashService.matches(refreshToken, session.getRefreshTokenHash())) {
            throw new GeneralException(GeneralErrorCode.UNAUTHORIZED);
        }
        session.revoke(clock.instant());
    }

    @Transactional(readOnly = true)
    public AuthMeResponse getMe(Long userId) {
        UserAccountSummary user = userAccountFacade.getAccount(userId);
        List<OAuthProvider> providers = oauthAccountRepository.findAllByUserId(userId).stream()
                .map(OAuthAccount::getProvider)
                .sorted(Comparator.comparing(Enum::name))
                .toList();
        return AuthMeResponse.of(user, providers);
    }

    public OnboardingResponse completeOnboarding(Long userId, OnboardingRequest request) {
        Instant now = clock.instant();
        UserAccountSummary user = userAccountFacade.completeOnboarding(
                userId,
                request.name(),
                request.studentNumber(),
                request.department(),
                request.email(),
                now
        );
        membershipLinkFacade.linkExistingMemberships(
                userId,
                user.name(),
                user.studentNumber(),
                now
        );
        return OnboardingResponse.from(user);
    }

    private OAuthAccount createOAuthAccount(OAuthProfile profile) {
        Long userId = userAccountFacade.createPendingUser();
        try {
            OAuthAccount account = oauthAccountRepository.save(OAuthAccount.create(
                    userId,
                    profile.provider(),
                    profile.providerUserId(),
                    profile.providerEmail()
            ));
            oauthAccountRepository.flush();
            return account;
        } catch (DataIntegrityViolationException ex) {
            throw new GeneralException(AuthErrorCode.OAUTH_ACCOUNT_CONFLICT);
        }
    }

    private TokenPair createSession(Long userId, String deviceInfo, String ipAddress) {
        UUID sessionKey = UUID.randomUUID();
        TokenPair tokens = jwtTokenService.issueTokenPair(userId, sessionKey);
        authSessionRepository.save(AuthSession.create(
                userId,
                sessionKey,
                tokenHashService.hash(tokens.refreshToken()),
                tokens.refreshTokenExpiresAt(),
                normalizeDeviceInfo(deviceInfo),
                ipAddress
        ));
        return tokens;
    }

    private void validateRedirectUri(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()
                || properties.allowedRedirectUris() == null
                || properties.allowedRedirectUris().stream().noneMatch(redirectUri::equals)) {
            throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        }
        try {
            URI uri = URI.create(redirectUri);
            if (!uri.isAbsolute() || uri.getHost() == null) {
                throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
            }
        } catch (IllegalArgumentException ex) {
            throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        }
    }

    private String normalizeDeviceInfo(String deviceInfo) {
        if (deviceInfo == null || deviceInfo.isBlank()) {
            return null;
        }
        return deviceInfo.length() <= 500 ? deviceInfo : deviceInfo.substring(0, 500);
    }

    private boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                right.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }
}
