package com.dongbang.global.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.dongbang.global.response.code.BaseErrorCode;
import com.dongbang.global.response.code.BaseSuccessCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result", "errorDetail"})
public class ApiResponse<T> {

    @Getter(AccessLevel.NONE)
    @JsonProperty("isSuccess")
    private final boolean isSuccess;

    private final String code;
    private final String message;

    private final T result;

    private final Object errorDetail;

    public static <T> ApiResponse<T> onSuccess(BaseSuccessCode code, T result) {
        return new ApiResponse<>(true, code.getCode(), code.getMessage(), result, null);
    }

    public static <T> ApiResponse<T> onFailure(BaseErrorCode code, Object errorDetail) {
        return new ApiResponse<>(false, code.getCode(), code.getMessage(), null, errorDetail);
    }
}
