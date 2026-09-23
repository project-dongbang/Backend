package com.dongbang.event.presentation;

import com.dongbang.event.application.EventParticipationService;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventParticipationController.class)
@Import({SecurityConfig.class, ApiSecurityExceptionHandler.class, WebMvcConfig.class, GeneralExceptionAdvice.class})
@WithMockUser(username = "7", roles = "USER")
class EventParticipationControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean EventParticipationService service;
    static final String BASE = "/api/v1/organizations/1/events/10";
    @Test void applyUsesAuthenticatedIdentity() throws Exception {
        mvc.perform(post(BASE + "/participants/me").with(csrf()).header("X-User-Id", "999"))
                .andExpect(status().isCreated());
        verify(service).apply(1L, 7L, 10L);
    }
    @Test void csrfIsRequired() throws Exception {
        mvc.perform(post(BASE + "/participants/me")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void anonymousCannotReadParticipants() throws Exception {
        mvc.perform(get(BASE + "/participants").with(anonymous())).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void missingVersionRejected() throws Exception {
        mvc.perform(post(BASE + "/participants").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"membershipId\":2}")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void negativeVersionRejected() throws Exception {
        mvc.perform(delete(BASE + "/participants/2").with(csrf()).param("participantVersion", "-1"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void staffAddPassesVersion() throws Exception {
        mvc.perform(post(BASE + "/participants").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"membershipId\":2,\"participantVersion\":4}"))
                .andExpect(status().isCreated());
        verify(service).addParticipant(1L, 7L, 10L, 2L, 4);
    }
    @Test void closeAndCancelRoutes() throws Exception {
        mvc.perform(post(BASE + "/registration/close").with(csrf())).andExpect(status().isOk());
        mvc.perform(post(BASE + "/cancel").with(csrf())).andExpect(status().isOk());
        verify(service).closeRegistration(1L, 7L, 10L);
        verify(service).cancel(1L, 7L, 10L);
    }
}
