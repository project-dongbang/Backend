package com.dongbang.global.config;

import com.dongbang.global.response.ApiResponse;
import com.dongbang.global.response.code.BaseSuccessCode;
import com.dongbang.global.response.code.GeneralSuccessCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;

import java.util.LinkedHashMap;

@Component
public class ApiSuccessExamplesCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (!returnsApiResponse(handlerMethod)) return operation;

        BaseSuccessCode success = successCode(handlerMethod);
        String status = String.valueOf(success.getHttpStatus().value());
        if (operation.getResponses() == null) operation.setResponses(new ApiResponses());
        var response = operation.getResponses().computeIfAbsent(status,
                key -> new io.swagger.v3.oas.models.responses.ApiResponse().description(success.getHttpStatus().getReasonPhrase()));
        if (response.getContent() == null) response.setContent(new Content());
        MediaType media = response.getContent().computeIfAbsent("application/json", key -> new MediaType());

        // 역할별 성공 예시 우선
        if (media.getExamples() != null && !media.getExamples().isEmpty()) return operation;

        var body = new LinkedHashMap<String, Object>();
        body.put("isSuccess", true);
        body.put("code", success.getCode());
        body.put("message", success.getMessage());
        body.put("result", null);
        body.put("errorDetail", null);
        media.addExamples("성공", new Example().summary(success.getMessage()).value(body));
        return operation;
    }

    private boolean returnsApiResponse(HandlerMethod handlerMethod) {
        ResolvableType returnType = ResolvableType.forMethodReturnType(handlerMethod.getMethod());
        Class<?> rawType = returnType.resolve();
        if (rawType != null && ApiResponse.class.isAssignableFrom(rawType)) return true;
        Class<?> bodyType = returnType.getGeneric(0).resolve();
        return bodyType != null && ApiResponse.class.isAssignableFrom(bodyType);
    }

    private BaseSuccessCode successCode(HandlerMethod handlerMethod) {
        ResponseStatus responseStatus = handlerMethod.getMethodAnnotation(ResponseStatus.class);
        if (responseStatus != null && responseStatus.value() == HttpStatus.CREATED) {
            return GeneralSuccessCode.CREATED;
        }
        return GeneralSuccessCode.OK;
    }
}
