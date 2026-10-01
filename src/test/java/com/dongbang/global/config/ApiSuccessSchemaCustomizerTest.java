package com.dongbang.global.config;

import com.dongbang.global.response.ApiResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiSuccessSchemaCustomizerTest {

    @Test
    void preservesRealDtoSchemaAndSetsSuccessFieldsPerResponse() throws Exception {
        var type = getClass().getDeclaredMethod("response").getGenericReturnType();
        var resolved = ModelConverters.getInstance().resolveAsResolvedSchema(new AnnotatedType(type).resolveAsRef(true));
        var components = new Components().schemas(resolved.referencedSchemas);
        var document = new OpenAPI().components(components).paths(new Paths());
        for (String status : List.of("200", "201", "400")) {
            document.getPaths().addPathItem("/" + status, new PathItem().get(new Operation().responses(
                    new ApiResponses().addApiResponse(status, new io.swagger.v3.oas.models.responses.ApiResponse()
                            .content(new Content().addMediaType("application/json", new MediaType().schema(resolved.schema)))))));
        }
        String originalComponents = Json.mapper().writeValueAsString(components);
        new ApiSuccessSchemaCustomizer().customise(document);

        var ok = media(document, "200");
        var created = media(document, "201");
        assertThat(ok.getExamples()).isNull();
        assertThat(ok.getExample()).isNull();
        Schema<?> okSchema = ok.getSchema();
        Schema<?> createdSchema = created.getSchema();
        assertThat(okSchema.getProperties().get("code").getExample()).isEqualTo("COMMON_200_001");
        assertThat(okSchema.getProperties().get("message").getExample()).isEqualTo("성공적으로 요청을 처리했습니다.");
        assertThat(createdSchema.getProperties().get("code").getExample()).isEqualTo("COMMON_201_001");
        var wrapper = components.getSchemas().get(resolved.schema.get$ref().replace("#/components/schemas/", ""));
        assertThat(Json.mapper().writeValueAsString(ok.getSchema().getProperties().get("result")))
                .isEqualTo(Json.mapper().writeValueAsString(wrapper.getProperties().get("result")));
        assertThat(media(document, "400").getSchema().get$ref()).isEqualTo(resolved.schema.get$ref());
        assertThat(Json.mapper().writeValueAsString(components)).isEqualTo(originalComponents);
        // 최종 OpenAPI JSON에도 DTO 참조와 성공 메시지 유지
        assertThat(Json.mapper().writeValueAsString(document)).contains("성공적으로 요청을 처리했습니다.", "eventId", "title");
    }

    @Test
    void preservesExplicitExamples() {
        var media = new MediaType().addExamples("운영진", new Example().value(Map.of("result", Map.of("amount", 40000))));
        var document = new OpenAPI().paths(new Paths().addPathItem("/200", new PathItem().get(new Operation()
                .responses(new ApiResponses().addApiResponse("200", new io.swagger.v3.oas.models.responses.ApiResponse()
                        .content(new Content().addMediaType("application/json", media)))))));
        new ApiSuccessSchemaCustomizer().customise(document);
        assertThat(media.getExamples()).containsOnlyKeys("운영진");
    }

    private MediaType media(OpenAPI document, String status) {
        return document.getPaths().get("/" + status).getGet().getResponses().get(status).getContent().get("application/json");
    }

    ApiResponse<List<EventExample>> response() { return null; }
    record EventExample(Long eventId, String title) {}
}
