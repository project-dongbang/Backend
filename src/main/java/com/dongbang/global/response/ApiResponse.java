package com.dongbang.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.dongbang.global.response.code.BaseErrorCode;
import com.dongbang.global.response.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result", "errorDetail"})
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    private final boolean isSuccess;

    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    // 실패 시에만 optional
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Object errorDetail;

    public static <T> ApiResponse<T> onSuccess(BaseSuccessCode code, T result) {
        return new ApiResponse<>(true, code.getCode(), code.getMessage(), result, null);
    }

    public static <T> ApiResponse<T> onFailure(BaseErrorCode code, Object errorDetail) {
        return new ApiResponse<>(false, code.getCode(), code.getMessage(), null, errorDetail);
    }
}
