package com.dongbang.receipt.exception;

import com.dongbang.global.response.code.BaseErrorCode;
import com.dongbang.global.response.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReceiptErrorCode implements BaseErrorCode {
    MEMBER_REQUIRED(HttpStatus.FORBIDDEN, "RECEIPT_403_001", "동아리 회원만 영수증을 등록할 수 있습니다."),
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "RECEIPT_400_001", "업로드할 영수증 파일이 비어 있습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "RECEIPT_400_002", "JPEG, PNG, WebP 이미지만 업로드할 수 있습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "RECEIPT_413_001", "영수증 이미지는 5MB 이하만 업로드할 수 있습니다."),
    OCR_FAILED(HttpStatus.BAD_GATEWAY, "RECEIPT_502_001", "영수증 OCR 처리에 실패했습니다."),
    STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "RECEIPT_500_001", "영수증 저장에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReason getReason() {
        return ErrorReason.builder().httpStatus(httpStatus).code(code).message(message).build();
    }
}
