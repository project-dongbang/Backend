package com.dongbang.event.presentation;

import com.dongbang.attendance.presentation.AttendanceController;
import com.dongbang.global.config.ApiErrorExamplesCustomizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ApiErrorExamplesTest {
    private final ApiErrorExamplesCustomizer customizer = new ApiErrorExamplesCustomizer();

    @Test
    void allSeventeenApisHaveValidErrorExamplesWithoutReplacingSuccess() {
        int count = 0;
        for (Class<?> controller : new Class<?>[]{EventController.class, EventParticipationController.class, AttendanceController.class}) {
            for (var method : controller.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Operation.class)) continue;
                count++;
                var success = new ApiResponse().description("성공");
                var operation = new io.swagger.v3.oas.models.Operation()
                        .responses(new ApiResponses().addApiResponse("200", success));
                customizer.customize(operation, new HandlerMethod(mock(controller), method));
                assertThat(operation.getResponses().get("200")).isSameAs(success);
                assertThat(operation.getResponses()).containsKeys("400", "401", "403", "404");
                operation.getResponses().forEach((status, response) -> {
                    if (status.equals("200")) return;
                    response.getContent().get("application/json").getExamples().values().forEach(example -> {
                        Map<?, ?> body = (Map<?, ?>) example.getValue();
                        assertThat(body.keySet().stream().map(Object::toString).toList())
                                .containsExactly("isSuccess", "code", "message", "result", "errorDetail");
                        assertThat(body.get("isSuccess")).isEqualTo(false);
                        assertThat(body.get("code").toString()).contains("_" + status + "_");
                        assertThat(body.get("message")).isNotNull();
                        assertThat(body.get("result")).isNull();
                        assertThat(body.get("errorDetail")).isNull();
                    });
                });
            }
        }
        assertThat(count).isEqualTo(17);
    }

    @Test
    void checkInIncludesExpiredAndAllConflictExamples() {
        var method = Arrays.stream(AttendanceController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("checkIn")).findFirst().orElseThrow();
        var operation = customizer.customize(new io.swagger.v3.oas.models.Operation(),
                new HandlerMethod(mock(AttendanceController.class), method));
        assertThat(operation.getResponses().get("410").getContent().get("application/json").getExamples())
                .containsKey("AttendanceErrorCode_QR_EXPIRED");
        assertThat(operation.getResponses().get("409").getContent().get("application/json").getExamples())
                .containsKeys("AttendanceErrorCode_SESSION_NOT_STARTED", "AttendanceErrorCode_SESSION_CLOSED",
                        "AttendanceErrorCode_ALREADY_PRESENT", "EventErrorCode_EVENT_CANCELED");
    }
}
