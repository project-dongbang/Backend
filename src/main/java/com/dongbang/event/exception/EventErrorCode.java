package com.dongbang.event.exception;

import com.dongbang.global.response.code.BaseErrorCode;
import com.dongbang.global.response.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EventErrorCode implements BaseErrorCode {

    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVT_404_001", "요청한 일정 또는 행사를 찾을 수 없습니다."),
    INVALID_EVENT_TIME(HttpStatus.BAD_REQUEST, "EVT_400_002", "시작·종료·신청 마감 시각이 올바르지 않습니다."),
    CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "EVT_409_006", "변경 후 참가자 수가 모집 정원을 초과합니다."),
    ATTENDANCE_ALREADY_STARTED(HttpStatus.CONFLICT, "EVT_409_007", "출석이 시작된 행사는 해당 내용을 변경할 수 없습니다.");

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
