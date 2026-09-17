package com.dongbang.photo.exception;

import com.dongbang.global.response.code.BaseErrorCode;
import com.dongbang.global.response.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PhotoErrorCode implements BaseErrorCode {

    PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "PHOTO_404_001", "요청하신 사진이 존재하지 않습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_404_001", "요청하신 파일이 존재하지 않습니다."),

    FILE_EMPTY(HttpStatus.BAD_REQUEST, "FILE_400_001", "업로드할 파일이 비어 있습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "FILE_400_002", "이미지 파일만 업로드할 수 있습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_500_001", "파일 업로드 처리에 실패했습니다."),

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
