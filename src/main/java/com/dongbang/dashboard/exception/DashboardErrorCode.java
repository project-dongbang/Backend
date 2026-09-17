package com.dongbang.dashboard.exception;

import com.dongbang.global.response.code.BaseErrorCode;
import com.dongbang.global.response.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DashboardErrorCode implements BaseErrorCode {

    ORGANIZATION_NOT_FOUND(HttpStatus.NOT_FOUND, "ORG_404_001", "요청하신 동아리가 존재하지 않습니다."),
    STAFF_REQUIRED(HttpStatus.FORBIDDEN, "AUTH_403_001", "운영진 이상의 권한이 필요합니다."),
    MEMBER_REQUIRED(HttpStatus.FORBIDDEN, "AUTH_403_002", "동아리 회원만 접근할 수 있습니다.");

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
