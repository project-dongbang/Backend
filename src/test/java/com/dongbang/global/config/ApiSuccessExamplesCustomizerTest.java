package com.dongbang.global.config;

import com.dongbang.global.response.ApiResponse;
import com.dongbang.global.response.code.GeneralSuccessCode;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ApiSuccessExamplesCustomizerTest {

    private final ApiSuccessExamplesCustomizer customizer = new ApiSuccessExamplesCustomizer();

    @Test
    void addsActualOkCodeAndMessageForCommonResponse() throws Exception {
        var operation = operation("200");
        customizer.customize(operation, handler("get"));

        Map<?, ?> body = (Map<?, ?>) operation.getResponses().get("200").getContent()
                .get("application/json").getExamples().get("성공").getValue();
        assertThat(body.get("isSuccess")).isEqualTo(true);
        assertThat(body.get("code")).isEqualTo("COMMON_200_001");
        assertThat(body.get("message")).isEqualTo("성공적으로 요청을 처리했습니다.");
        assertThat(body.get("result")).isNull();
        assertThat(body.get("errorDetail")).isNull();
    }

    @Test
    void addsCreatedCodeAndMessageForCreatedResponse() throws Exception {
        var operation = operation("201");
        customizer.customize(operation, handler("create"));

        Map<?, ?> body = (Map<?, ?>) operation.getResponses().get("201").getContent()
                .get("application/json").getExamples().get("성공").getValue();
        assertThat(body.get("code")).isEqualTo("COMMON_201_001");
        assertThat(body.get("message")).isEqualTo("리소스가 성공적으로 생성되었습니다.");
    }

    @Test
    void preservesSpecificSuccessExamples() throws Exception {
        var operation = operation("200");
        operation.getResponses().get("200").getContent().get("application/json")
                .addExamples("운영진", new Example().value(Map.of("code", GeneralSuccessCode.OK.getCode())));

        customizer.customize(operation, handler("get"));

        assertThat(operation.getResponses().get("200").getContent().get("application/json").getExamples())
                .containsOnlyKeys("운영진");
    }

    private HandlerMethod handler(String name) throws Exception {
        return new HandlerMethod(mock(TestController.class), TestController.class.getDeclaredMethod(name));
    }

    private io.swagger.v3.oas.models.Operation operation(String status) {
        var media = new MediaType();
        var response = new io.swagger.v3.oas.models.responses.ApiResponse()
                .content(new Content().addMediaType("application/json", media));
        return new io.swagger.v3.oas.models.Operation().responses(new ApiResponses().addApiResponse(status, response));
    }

    static class TestController {
        ApiResponse<String> get() {
            return ApiResponse.onSuccess(GeneralSuccessCode.OK, "ok");
        }

        @ResponseStatus(HttpStatus.CREATED)
        ResponseEntity<ApiResponse<Long>> create() {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.onSuccess(GeneralSuccessCode.CREATED, 1L));
        }
    }
}
