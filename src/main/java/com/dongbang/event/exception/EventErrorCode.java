package com.dongbang.event.exception;

import com.dongbang.global.response.code.BaseErrorCode;
import com.dongbang.global.response.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EventErrorCode implements BaseErrorCode {

    EVENT_ONLY(HttpStatus.BAD_REQUEST, "EVT_400_001", "일반 일정에서는 사용할 수 없는 기능입니다."),
    INVALID_PARTICIPANT_CHANGE(HttpStatus.BAD_REQUEST, "EVT_400_003", "유효하지 않은 참가자 변경 요청입니다."),
    REGISTRATION_CLOSED(HttpStatus.CONFLICT, "EVT_409_001", "참가 신청이 마감되었습니다."),
    REGISTRATION_FULL(HttpStatus.CONFLICT, "EVT_409_002", "행사 모집 정원이 마감되었습니다."),
    ALREADY_PARTICIPATING(HttpStatus.CONFLICT, "EVT_409_003", "이미 행사 참가자로 등록되어 있습니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVT_404_002", "취소할 참가 내역이 없습니다."),
    VERSION_CONFLICT(HttpStatus.CONFLICT, "EVT_409_005", "참가자 명단이 변경되었습니다. 다시 조회해 주세요."),
    EVENT_CANCELED(HttpStatus.CONFLICT, "EVT_409_004", "취소된 행사입니다."),

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
