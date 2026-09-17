package com.dongbang.auth.exception;

import com.dongbang.global.response.code.BaseErrorCode;
import com.dongbang.global.response.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    INVALID_OAUTH_REQUEST(HttpStatus.BAD_REQUEST, "AUTH_400_002", "OAuth 인증 요청이 만료되었거나 유효하지 않습니다."),
    OAUTH_AUTHENTICATION_FAILED(HttpStatus.BAD_REQUEST, "AUTH_400_003", "OAuth 인증에 실패했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_002", "Refresh Token이 만료되었거나 유효하지 않습니다."),
    OAUTH_ACCOUNT_CONFLICT(HttpStatus.CONFLICT, "AUTH_409_001", "이미 다른 사용자에게 연결된 소셜 계정입니다."),
    ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "AUTH_409_002", "이미 회원가입 추가정보 입력을 완료했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReason getReason() {
        return ErrorReason.builder()
                .httpStatus(httpStatus)
                .code(code)
                .message(message)
                .build();
    }
}
