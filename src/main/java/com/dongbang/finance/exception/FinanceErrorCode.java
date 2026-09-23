package com.dongbang.finance.exception;

import com.dongbang.global.response.code.BaseErrorCode;
import com.dongbang.global.response.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FinanceErrorCode implements BaseErrorCode {
    INVALID_TARGET(HttpStatus.BAD_REQUEST, "FEE_400_001", "납부 대상에 포함할 수 없는 멤버가 있습니다."),
    FEE_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "FEE_404_001", "납부 항목을 찾을 수 없습니다."),
    FEE_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "FEE_404_002", "회원별 납부 대상을 찾을 수 없습니다."),
    DUPLICATE_TARGET(HttpStatus.CONFLICT, "FEE_409_001", "이미 다른 납부 카테고리에 포함된 회원입니다."),
    INVALID_STATUS_CHANGE(HttpStatus.CONFLICT, "FEE_409_003", "현재 상태에서는 요청한 납부 상태로 변경할 수 없습니다."),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "LEDGER_404_001", "장부 거래를 찾을 수 없습니다."),
    DUPLICATE_EVIDENCE(HttpStatus.CONFLICT, "LEDGER_409_001", "이미 등록된 영수증입니다."),
    VOID_TRANSACTION(HttpStatus.CONFLICT, "LEDGER_409_002", "무효화된 거래는 수정할 수 없습니다."),
    AUTO_INCOME(HttpStatus.CONFLICT, "LEDGER_409_004", "회원 납부로 생성된 수입 내역은 직접 수정할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override public ErrorReason getReason() {
        return ErrorReason.builder().httpStatus(httpStatus).code(code).message(message).build();
    }
}
