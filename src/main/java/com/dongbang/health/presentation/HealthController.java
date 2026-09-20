package com.dongbang.health.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "헬스체크", description = "서버 상태 확인 API")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(summary = "서버 상태 확인", description = "DongBang API 서버의 실행 상태를 확인합니다.")
    public HealthResponse health() {
        return new HealthResponse("dongbang-api", "UP");
    }

    public record HealthResponse(String service, String status) {
    }
}
