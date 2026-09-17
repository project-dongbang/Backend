package com.dongbang.auth.presentation;

import com.dongbang.auth.application.AuthApplicationService;
import com.dongbang.auth.application.OAuthAuthorizationResult;
import com.dongbang.auth.application.OAuthLoginResult;
import com.dongbang.auth.application.token.TokenPair;
import com.dongbang.auth.domain.OAuthProvider;
import com.dongbang.auth.infrastructure.token.JwtTokenService;
import com.dongbang.auth.infrastructure.web.AuthCookieService;
import com.dongbang.auth.presentation.dto.request.OnboardingRequest;
import com.dongbang.auth.presentation.dto.response.AuthMeResponse;
import com.dongbang.auth.presentation.dto.response.OnboardingResponse;
import com.dongbang.auth.presentation.dto.response.RefreshTokenResponse;
import com.dongbang.global.response.ApiResponse;
import com.dongbang.global.response.code.GeneralSuccessCode;
import com.dongbang.global.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "인증", description = "Google/Kakao OAuth 로그인, 토큰 및 최초 회원가입 API")
public class AuthController {

    private final AuthApplicationService authService;
    private final AuthCookieService cookieService;
    private final JwtTokenService jwtTokenService;

    @Operation(summary = "Google OAuth 인증 URL 생성")
    @GetMapping("/oauth/google/authorize")
    public ResponseEntity<Void> authorizeGoogle(
            @RequestParam @NotBlank String redirectUri
    ) {
        return authorizationResponse(OAuthProvider.GOOGLE, redirectUri);
    }

    @Operation(summary = "Kakao OAuth 인증 URL 생성")
    @GetMapping("/oauth/kakao/authorize")
    public ResponseEntity<Void> authorizeKakao(
            @RequestParam @NotBlank String redirectUri
    ) {
        return authorizationResponse(OAuthProvider.KAKAO, redirectUri);
    }

    @Operation(summary = "Google OAuth 콜백 처리 및 로그인")
    @GetMapping("/oauth/google/callback")
    public ResponseEntity<Void> callbackGoogle(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            @CookieValue(name = "oauth_state_google", required = false) String stateCookie,
            HttpServletRequest request
    ) {
        return callbackResponse(OAuthProvider.GOOGLE, code, state, error, stateCookie, request);
    }

    @Operation(summary = "Kakao OAuth 콜백 처리 및 로그인")
    @GetMapping("/oauth/kakao/callback")
    public ResponseEntity<Void> callbackKakao(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            @CookieValue(name = "oauth_state_kakao", required = false) String stateCookie,
            HttpServletRequest request
    ) {
        return callbackResponse(OAuthProvider.KAKAO, code, state, error, stateCookie, request);
    }

    @Operation(summary = "Access Token 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @Parameter(hidden = true)
            @CookieValue(name = AuthCookieService.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletRequest request
    ) {
        TokenPair tokens = authService.refresh(refreshToken, userAgent(request), request.getRemoteAddr());
        ApiResponse<RefreshTokenResponse> body = ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                new RefreshTokenResponse(jwtTokenService.accessTokenExpiresInSeconds())
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.accessToken(tokens).toString())
                .header(HttpHeaders.SET_COOKIE, cookieService.refreshToken(tokens).toString())
                .body(body);
    }

    @Operation(summary = "현재 로그인 세션 로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true)
            @CookieValue(name = AuthCookieService.REFRESH_TOKEN_COOKIE, required = false) String refreshToken
    ) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.clearAccessToken().toString())
                .header(HttpHeaders.SET_COOKIE, cookieService.clearRefreshToken().toString())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, null));
    }

    @Operation(summary = "내 인증·회원가입 상태 조회")
    @GetMapping("/me")
    public ApiResponse<AuthMeResponse> getMe(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(hidden = true) CsrfToken csrfToken
    ) {
        csrfToken.getToken();
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, authService.getMe(userId));
    }

    @Operation(summary = "최초 회원가입 추가정보 저장")
    @PutMapping("/onboarding")
    public ApiResponse<OnboardingResponse> onboarding(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Valid @RequestBody OnboardingRequest request
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, authService.completeOnboarding(userId, request));
    }

    private ResponseEntity<Void> authorizationResponse(OAuthProvider provider, String redirectUri) {
        OAuthAuthorizationResult result = authService.createAuthorization(provider, redirectUri);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(result.authorizationUri())
                .header(HttpHeaders.SET_COOKIE, cookieService.oauthState(provider, result.stateToken()).toString())
                .build();
    }

    private ResponseEntity<Void> callbackResponse(
            OAuthProvider provider,
            String code,
            String state,
            String error,
            String stateCookie,
            HttpServletRequest request
    ) {
        OAuthLoginResult result = authService.completeOAuthLogin(
                provider,
                code,
                state,
                stateCookie,
                error,
                userAgent(request),
                request.getRemoteAddr()
        );
        URI destination = UriComponentsBuilder.fromUri(result.redirectUri())
                .queryParam("onboardingRequired", result.onboardingRequired())
                .build(true)
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(destination)
                .header(HttpHeaders.SET_COOKIE, cookieService.accessToken(result.tokens()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieService.refreshToken(result.tokens()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieService.clearOAuthState(provider).toString())
                .build();
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.USER_AGENT);
    }
}
