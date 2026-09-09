package com.dongbang.health.presentation;

import com.dongbang.global.config.SecurityConfig;
import com.dongbang.global.security.ApiSecurityExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, ApiSecurityExceptionHandler.class})
class HealthControllerTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("dongbang-api"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void businessApiIsClosedUntilAuthenticationIsImplemented() throws Exception {
        mvc.perform(get("/api/members"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_401_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.result").doesNotExist())
                .andExpect(jsonPath("$.errorDetail").doesNotExist());
    }

    @Test
    void actuatorInternalsAreNotPublic() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void evenAuthenticatedUsersCannotAccessUnconfiguredBusinessApi() throws Exception {
        mvc.perform(get("/api/members")).andExpect(status().isForbidden());
    }

    @Test
    void csrfProtectionIsNotDisabledByTheInfrastructureSetup() throws Exception {
        mvc.perform(post("/api/members"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_403_001"))
                .andExpect(jsonPath("$.message").value("요청이 거부되었습니다."));
    }
}
