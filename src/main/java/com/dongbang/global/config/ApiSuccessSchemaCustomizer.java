package com.dongbang.global.config;

import com.dongbang.global.response.code.GeneralSuccessCode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

@Component
public class ApiSuccessSchemaCustomizer implements OpenApiCustomizer {

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null) return;
        openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
            if (operation.getResponses() == null) return;
            operation.getResponses().forEach((status, response) -> {
                GeneralSuccessCode success = switch (status) {
                    case "200" -> GeneralSuccessCode.OK;
                    case "201" -> GeneralSuccessCode.CREATED;
                    default -> null;
                };
                if (success == null || response.getContent() == null) return;
                response.getContent().values().forEach(media -> {
                    // API별 명시적 예시 우선
                    if (media.getExample() != null || (media.getExamples() != null && !media.getExamples().isEmpty())) return;
                    Schema<?> schema = resolve(openApi, media.getSchema());
                    if (schema == null || schema.getProperties() == null
                            || !schema.getProperties().keySet().containsAll(java.util.Set.of("isSuccess", "code", "message", "result", "errorDetail"))) return;

                    // 응답별 복사: 공용 DTO 및 오류 응답 스키마 보존
                    Schema<?> copy = Json.mapper().convertValue(schema, Schema.class);
                    copy.getProperties().get("isSuccess").setExample(true);
                    copy.getProperties().get("code").setExample(success.getCode());
                    copy.getProperties().get("message").setExample(success.getMessage());
                    media.setSchema(copy);
                });
            });
        }));
    }

    private Schema<?> resolve(OpenAPI openApi, Schema<?> schema) {
        if (schema == null || schema.get$ref() == null) return schema;
        String prefix = "#/components/schemas/";
        if (!schema.get$ref().startsWith(prefix) || openApi.getComponents() == null
                || openApi.getComponents().getSchemas() == null) return null;
        return openApi.getComponents().getSchemas().get(schema.get$ref().substring(prefix.length()));
    }
}
