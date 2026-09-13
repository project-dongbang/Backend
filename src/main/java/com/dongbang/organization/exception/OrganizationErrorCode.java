package com.dongbang.organization.exception;

import com.dongbang.global.response.code.BaseErrorCode;
import com.dongbang.global.response.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrganizationErrorCode implements BaseErrorCode {

    ORGANIZATION_NOT_FOUND(HttpStatus.NOT_FOUND, "ORG_404_001", "요청하신 동아리가 존재하지 않습니다."),
    INVALID_INVITATION_TOKEN(HttpStatus.NOT_FOUND, "ORG_404_002", "유효하지 않거나 만료된 초대 코드입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORG_404_003", "해당 회원이 동아리에 존재하지 않습니다."),

    ALREADY_JOINED_MEMBER(HttpStatus.CONFLICT, "ORG_409_001", "이미 해당 동아리에 가입된 회원입니다."),
    SLUG_ALREADY_EXISTS(HttpStatus.CONFLICT, "ORG_409_002", "이미 사용 중인 동아리 슬러그입니다."),

    INVALID_DELEGATION_TARGET(HttpStatus.BAD_REQUEST, "ORG_400_001", "운영진(ADMIN)에게만 대표 권한을 위임할 수 있습니다."),
    OWNER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "ORG_400_002", "회장은 대표 권한을 위임하기 전까지 탈퇴할 수 없습니다."),

    STAFF_REQUIRED(HttpStatus.FORBIDDEN, "AUTH_403_001", "운영진 이상의 권한이 필요합니다."),
    OWNER_REQUIRED(HttpStatus.FORBIDDEN, "AUTH_403_002", "회장(OWNER) 권한이 필요합니다.");

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
