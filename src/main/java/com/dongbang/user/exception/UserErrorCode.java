package com.dongbang.user.exception;

import com.dongbang.global.response.code.BaseErrorCode;
import com.dongbang.global.response.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_001", "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409_001", "이미 사용 중인 이메일입니다."),
    STUDENT_NUMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409_002", "이미 사용 중인 학번입니다.");

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
