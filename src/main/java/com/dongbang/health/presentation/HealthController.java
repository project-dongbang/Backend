package com.dongbang.health.presentation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("dongbang-api", "UP");
    }

    public record HealthResponse(String service, String status) {
    }
}
