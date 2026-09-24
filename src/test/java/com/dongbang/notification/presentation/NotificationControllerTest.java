package com.dongbang.notification.presentation;

import com.dongbang.auth.infrastructure.token.JwtTokenService;
import com.dongbang.global.config.SecurityConfig;
import com.dongbang.global.config.WebMvcConfig;
import com.dongbang.global.security.ApiSecurityExceptionHandler;
import com.dongbang.notification.application.NotificationService;
import com.dongbang.notification.domain.NotificationReferenceType;
import com.dongbang.notification.domain.NotificationType;
import com.dongbang.notification.presentation.dto.NotificationDtos.NotificationItem;
import com.dongbang.notification.presentation.dto.NotificationDtos.NotificationListResult;
import com.dongbang.notification.presentation.dto.NotificationDtos.NotificationReadAllResult;
import com.dongbang.notification.presentation.dto.NotificationDtos.NotificationReadResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, ApiSecurityExceptionHandler.class, WebMvcConfig.class})
class NotificationControllerTest {

    @Autowired private MockMvc mvc;

    @MockitoBean private NotificationService notificationService;
    @MockitoBean private JwtTokenService jwtTokenService;

    @Test
    void getsNotificationsWithUnreadCountAndSentAt() throws Exception {
        given(notificationService.getNotifications(1L, 10L, false, null, 20))
                .willReturn(new NotificationListResult(
                        1,
                        List.of(new NotificationItem(
                                501L,
                                NotificationType.FEE_DUE_REMINDER,
                                "납부 마감 하루 전입니다.",
                                "납부할 금액은 40,000원입니다.",
                                NotificationReferenceType.FEE_ITEM,
                                21L,
                                false,
                                null,
                                Instant.parse("2026-09-24T00:00:00Z")
                        )),
                        20,
                        null,
                        false
                ));

        mvc.perform(get("/api/v1/notifications")
                        .with(user("10").roles("USER"))
                        .queryParam("organizationId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON_200_001"))
                .andExpect(jsonPath("$.result.unreadCount").value(1))
                .andExpect(jsonPath("$.result.content[0].notificationType").value("FEE_DUE_REMINDER"))
                .andExpect(jsonPath("$.result.content[0].isRead").value(false))
                .andExpect(jsonPath("$.result.content[0].readAt").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.result.content[0].sentAt").value("2026-09-24T00:00:00Z"));
    }

    @Test
    void marksNotificationAsRead() throws Exception {
        given(notificationService.markRead(501L, 10L)).willReturn(new NotificationReadResult(
                501L, true, Instant.parse("2026-09-24T01:00:00Z")));

        mvc.perform(patch("/api/v1/notifications/501/read")
                        .with(user("10").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.notificationId").value(501))
                .andExpect(jsonPath("$.result.isRead").value(true));
    }

    @Test
    void marksCurrentOrganizationsNotificationsAsRead() throws Exception {
        given(notificationService.markAllRead(1L, 10L))
                .willReturn(new NotificationReadAllResult(3, 0));

        mvc.perform(post("/api/v1/notifications/read-all")
                        .with(user("10").roles("USER"))
                        .with(csrf())
                        .queryParam("organizationId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.updatedCount").value(3))
                .andExpect(jsonPath("$.result.unreadCount").value(0));
    }
}
