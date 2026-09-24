package com.dongbang.attendance.exception;

import com.dongbang.global.response.code.BaseErrorCode;
import com.dongbang.global.response.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AttendanceErrorCode implements BaseErrorCode {
    INVALID_QR(HttpStatus.BAD_REQUEST, "ATT_400_001", "유효하지 않은 QR 코드입니다."),
    DIFFERENT_EVENT_QR(HttpStatus.BAD_REQUEST, "ATT_400_002", "다른 동아리 또는 행사의 QR 코드입니다."),
    PARTICIPANT_ONLY(HttpStatus.FORBIDDEN, "ATT_403_001", "행사 참가자만 출석할 수 있습니다."),
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "ATT_404_001", "출석 기록을 찾을 수 없습니다."),
    SESSION_NOT_STARTED(HttpStatus.CONFLICT, "ATT_409_001", "출석이 시작되지 않았습니다."),
    ALREADY_GENERATED(HttpStatus.CONFLICT, "ATT_409_002", "이미 QR 코드가 생성된 행사입니다."),
    SESSION_CLOSED(HttpStatus.CONFLICT, "ATT_409_003", "출석이 종료되었습니다."),
    ALREADY_PRESENT(HttpStatus.CONFLICT, "ATT_409_004", "이미 출석 처리되었습니다."),
    VERSION_CONFLICT(HttpStatus.CONFLICT, "ATT_409_005", "출석 기록이 변경되었습니다. 다시 조회해 주세요."),
    QR_EXPIRED(HttpStatus.GONE, "ATT_410_001", "QR 코드가 만료되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReason getReason() {
        return ErrorReason.builder().httpStatus(httpStatus).code(code).message(message).build();
    }
}
