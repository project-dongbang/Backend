package com.dongbang.event.presentation;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.datasource.url=${ATTENDANCE_TEST_DB_URL:${DB_URL:jdbc:postgresql://localhost:55459/attendance_test}}",
        "spring.datasource.username=${ATTENDANCE_TEST_DB_USERNAME:${DB_USERNAME:attendance_test}}",
        "spring.datasource.password=${ATTENDANCE_TEST_DB_PASSWORD:${DB_PASSWORD:isolated_test_only}}"
})
class ApiErrorExamplesIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void openApiPublishesQrExpirationExample() throws Exception {
        String example = "$.paths['/api/v1/organizations/{organizationId}/events/{eventId}/attendance/check-in']"
                + ".post.responses['410'].content['application/json'].examples.AttendanceErrorCode_QR_EXPIRED.value";
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(example + ".isSuccess").value(false))
                .andExpect(jsonPath(example + ".code").value("ATT_410_001"))
                .andExpect(jsonPath(example + ".result").hasJsonPath())
                .andExpect(jsonPath(example + ".errorDetail").hasJsonPath());
    }
}
