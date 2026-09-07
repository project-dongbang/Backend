package com.dongbang.global.apiPayload.handler;

import com.dongbang.global.apiPayload.ApiResponse;
import com.dongbang.global.apiPayload.code.GeneralErrorCode;
import com.dongbang.global.apiPayload.code.GeneralSuccessCode;
import com.dongbang.global.apiPayload.exception.GeneralException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({GeneralExceptionAdvice.class, GeneralExceptionAdviceTest.TestController.class})
@WithMockUser
class GeneralExceptionAdviceTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void returnsUnifiedSuccessResponse() throws Exception {
        mvc.perform(get("/test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON_200_001"))
                .andExpect(jsonPath("$.message").value("성공적으로 요청을 처리했습니다."))
                .andExpect(jsonPath("$.result").value("ok"))
                .andExpect(jsonPath("$.errorDetail").doesNotExist());
    }

    @Test
    void handlesInvalidRequestBody() throws Exception {
        mvc.perform(post("/test/body")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400_002"))
                .andExpect(jsonPath("$.errorDetail[0]").value("name: 이름은 필수입니다."));
    }

    @Test
    void handlesRequestParameterConstraintViolation() throws Exception {
        mvc.perform(get("/test/page").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400_002"))
                .andExpect(jsonPath("$.errorDetail[0]").value("page: 페이지는 양수여야 합니다."));
    }

    @Test
    void handlesTypeMismatchWithoutEchoingTheRejectedValue() throws Exception {
        mvc.perform(get("/test/id").param("id", "secret-value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_002"))
                .andExpect(jsonPath("$.errorDetail[0]").value("id: 타입이 올바르지 않습니다."))
                .andExpect(content().string(not(containsString("secret-value"))));
    }

    @Test
    void handlesMissingRequiredParameter() throws Exception {
        mvc.perform(get("/test/id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_002"))
                .andExpect(jsonPath("$.errorDetail[0]").value("id: 필수 파라미터가 누락되었습니다."));
    }

    @Test
    void handlesMalformedJson() throws Exception {
        mvc.perform(post("/test/body")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_001"))
                .andExpect(jsonPath("$.errorDetail[0]")
                        .value("요청 본문(JSON)을 올바르게 작성해 주세요."));
    }

    @Test
    void handlesUnsupportedMethod() throws Exception {
        mvc.perform(put("/test/body")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"dongbang\"}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("COMMON_405_001"));
    }

    @Test
    void handlesUnsupportedContentType() throws Exception {
        mvc.perform(post("/test/body")
                        .with(csrf())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("dongbang"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("COMMON_415_001"));
    }

    @Test
    void handlesUnknownApiAsNotFound() throws Exception {
        mvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_404_001"));
    }

    @Test
    void handlesCustomException() throws Exception {
        mvc.perform(get("/test/custom-error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_001"))
                .andExpect(jsonPath("$.errorDetail").value("공개 가능한 상세 사유"));
    }

    @Test
    void hidesUnexpectedExceptionDetails() throws Exception {
        mvc.perform(get("/test/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("COMMON_500_001"))
                .andExpect(jsonPath("$.errorDetail").doesNotExist())
                .andExpect(content().string(not(containsString("internal-secret"))));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/success")
        ApiResponse<String> success() {
            return ApiResponse.onSuccess(GeneralSuccessCode.OK, "ok");
        }

        @PostMapping(value = "/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<String> body(@Valid @RequestBody TestRequest request) {
            return ApiResponse.onSuccess(GeneralSuccessCode.OK, request.name());
        }

        @GetMapping("/page")
        ApiResponse<Integer> page(
                @RequestParam @Positive(message = "페이지는 양수여야 합니다.") int page
        ) {
            return ApiResponse.onSuccess(GeneralSuccessCode.OK, page);
        }

        @GetMapping("/id")
        ApiResponse<Long> id(@RequestParam Long id) {
            return ApiResponse.onSuccess(GeneralSuccessCode.OK, id);
        }

        @GetMapping("/custom-error")
        void customError() {
            throw new GeneralException(GeneralErrorCode.BAD_REQUEST, "공개 가능한 상세 사유");
        }

        @GetMapping("/unexpected-error")
        void unexpectedError() {
            throw new IllegalStateException("internal-secret");
        }
    }

    record TestRequest(@NotBlank(message = "이름은 필수입니다.") String name) {
    }
}
