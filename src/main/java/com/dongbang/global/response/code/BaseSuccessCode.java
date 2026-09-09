package com.dongbang.global.response.code;

import org.springframework.http.HttpStatus;

public interface BaseSuccessCode extends BaseCode{

    HttpStatus getHttpStatus();
    SuccessReason getReason();

}
