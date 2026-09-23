package com.dongbang.global.response.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FileErrorCode implements BaseErrorCode {
    INVALID_TYPE(HttpStatus.BAD_REQUEST, "FILE_400_001", "허용하지 않는 파일 형식입니다."),
    TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_413_001", "파일 크기가 허용 범위를 초과했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReason getReason() {
        return ErrorReason.builder().httpStatus(httpStatus).code(code).message(message).build();
    }
}
