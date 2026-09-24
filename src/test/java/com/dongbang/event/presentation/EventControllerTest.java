package com.dongbang.event.presentation;

import com.dongbang.event.application.EventCommandService;
import com.dongbang.event.application.EventQueryService;
import com.dongbang.event.application.command.UpdateEventCommand;
import com.dongbang.event.application.result.CalendarResult;
import com.dongbang.global.config.SecurityConfig;
import com.dongbang.global.config.WebMvcConfig;
import com.dongbang.global.exception.GeneralExceptionAdvice;
import com.dongbang.global.security.ApiSecurityExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, ApiSecurityExceptionHandler.class, WebMvcConfig.class, GeneralExceptionAdvice.class})
@WithMockUser(username = "1", roles = "USER")
class EventControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean EventCommandService commands;
    @MockitoBean EventQueryService queries;

    @Test
    void calendarReturnsCommonResponse() throws Exception {
        when(queries.calendar(1L, 7L, 2026, 9)).thenReturn(new CalendarResult(2026, 9, List.of()));
        mvc.perform(get("/api/v1/organizations/1/calendar")
                        .with(user("7").roles("USER")).param("year", "2026").param("month", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON_200_001"))
                .andExpect(jsonPath("$.result.events").isArray());
    }

    @Test
    void rejectsInvalidMonth() throws Exception {
        mvc.perform(get("/api/v1/organizations/1/calendar").param("year", "2026").param("month", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_002"));
        verifyNoInteractions(queries);
    }

    @Test
    void createReturns201() throws Exception {
        when(commands.create(eq(1L), eq(1L), any())).thenReturn(101L);
        mvc.perform(post("/api/v1/organizations/1/events").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EVENT","title":"행사","location":"동아리방",
                                 "startsAt":"2026-09-25T18:00:00+09:00","endsAt":"2026-09-25T20:00:00+09:00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMMON_201_001"))
                .andExpect(jsonPath("$.result.eventId").value(101));
    }

    @Test
    void patchPreservesExplicitNull() throws Exception {
        doAnswer(invocation -> {
            UpdateEventCommand patch = invocation.getArgument(3);
            assertThat(patch.isValidPatch()).isTrue();
            return null;
        }).when(commands).update(eq(1L), eq(1L), eq(101L), any());
        mvc.perform(patch("/api/v1/organizations/1/events/101").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"description\":null,\"capacity\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").hasJsonPath())
                .andExpect(jsonPath("$.result").value(org.hamcrest.Matchers.nullValue()));
        verify(commands).update(eq(1L), eq(1L), eq(101L), any());
    }

    @Test
    void rejectsEmptyPatch() throws Exception {
        mvc.perform(patch("/api/v1/organizations/1/events/101").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(commands);
    }

    @Test
    void rejectsNullRequiredPatchField() throws Exception {
        mvc.perform(patch("/api/v1/organizations/1/events/101").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"startsAt\":null}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(commands);
    }

    @Test
    void rejectsChangingEventType() throws Exception {
        mvc.perform(patch("/api/v1/organizations/1/events/101").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"SCHEDULE\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(commands);
    }

    @Test
    void deleteHasNoResult() throws Exception {
        mvc.perform(delete("/api/v1/organizations/1/events/101").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result").hasJsonPath())
                .andExpect(jsonPath("$.result").value(org.hamcrest.Matchers.nullValue()));
    }
    @Test
    @org.springframework.security.test.context.support.WithAnonymousUser
    void rejectsHeaderWithoutAuthentication() throws Exception {
        mvc.perform(get("/api/v1/organizations/1/calendar")
                        .header("X-User-Id", "7").param("year", "2026").param("month", "9"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_001"));
        verifyNoInteractions(queries);
    }

    @Test
    void authenticatedUserTakesPrecedenceOverLegacyHeader() throws Exception {
        when(queries.calendar(1L, 1L, 2026, 9)).thenReturn(new CalendarResult(2026, 9, List.of()));
        mvc.perform(get("/api/v1/organizations/1/calendar")
                        .header("X-User-Id", "999").param("year", "2026").param("month", "9"))
                .andExpect(status().isOk());
        verify(queries).calendar(1L, 1L, 2026, 9);
    }

    @Test
    @WithMockUser(username = "1", roles = "ONBOARDING")
    void rejectsUserBeforeOnboarding() throws Exception {
        mvc.perform(get("/api/v1/organizations/1/calendar").param("year", "2026").param("month", "9"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403_001"));
        verifyNoInteractions(queries);
    }

    @Test
    void rejectsMutationWithoutCsrf() throws Exception {
        mvc.perform(delete("/api/v1/organizations/1/events/101"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_403_001"));
        verifyNoInteractions(commands);
    }
}
