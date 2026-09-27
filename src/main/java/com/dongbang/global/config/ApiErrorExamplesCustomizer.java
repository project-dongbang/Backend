package com.dongbang.global.config;

import com.dongbang.global.response.ApiErrorExamples;
import com.dongbang.global.response.code.BaseErrorCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

@Component
public class ApiErrorExamplesCustomizer implements OperationCustomizer {
    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        Stream.concat(
                Arrays.stream(handlerMethod.getBeanType().getAnnotationsByType(ApiErrorExamples.class)),
                Arrays.stream(handlerMethod.getMethod().getAnnotationsByType(ApiErrorExamples.class)))
                .forEach(annotation -> {
                    for (String name : annotation.names()) {
                        BaseErrorCode error = Arrays.stream(annotation.value().getEnumConstants())
                                .filter(value -> ((Enum<?>) value).name().equals(name))
                                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                                        "Unknown API error: " + annotation.value().getSimpleName() + "." + name));
                        addExample(operation, annotation.value().getSimpleName() + "_" + name, error);
                    }
                });
        return operation;
    }

    private void addExample(Operation operation, String name, BaseErrorCode error) {
        if (operation.getResponses() == null) operation.setResponses(new ApiResponses());
        String status = String.valueOf(error.getHttpStatus().value());
        ApiResponse response = operation.getResponses().computeIfAbsent(status,
                key -> new ApiResponse().description(error.getHttpStatus().getReasonPhrase()));
        if (response.getContent() == null) response.setContent(new Content());
        MediaType media = response.getContent().computeIfAbsent("application/json", key -> new MediaType());
        // 실제 공통 응답과 동일한 필드 및 오류 코드 사용
        var body = new LinkedHashMap<String, Object>();
        body.put("isSuccess", false);
        body.put("code", error.getCode());
        body.put("message", error.getMessage());
        body.put("result", null);
        body.put("errorDetail", null);
        media.addExamples(name, new Example().summary(error.getMessage()).value(body));
    }
}
