package com.dongbang.global.response.code;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
public class SuccessReason {

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
