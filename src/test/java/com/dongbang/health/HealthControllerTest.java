package com.dongbang.health;

import com.dongbang.config.SecurityConfig;
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
@Import(SecurityConfig.class)
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
        mvc.perform(get("/api/members")).andExpect(status().isUnauthorized());
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
        mvc.perform(post("/api/members")).andExpect(status().isForbidden());
    }
}
