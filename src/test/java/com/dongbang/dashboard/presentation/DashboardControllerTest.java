package com.dongbang.dashboard.presentation;

import com.dongbang.dashboard.application.DashboardQueryService;
import com.dongbang.dashboard.application.port.DashboardScheduleItem;
import com.dongbang.dashboard.presentation.dto.*;
import com.dongbang.global.config.SecurityConfig;
import com.dongbang.global.config.WebMvcConfig;
import com.dongbang.global.security.ApiSecurityExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@Import({SecurityConfig.class, ApiSecurityExceptionHandler.class, WebMvcConfig.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DashboardQueryService dashboardQueryService;

    private final Long orgId = 1L;

    @Test
    @DisplayName("운영진 대시보드 조회 API 호출 성공")
    void getAdminDashboard() throws Exception {
        DashboardStatsResponse stats = new DashboardStatsResponse(64L, 8, 82.0, 91.0);
        DashboardScheduleItem schedule = new DashboardScheduleItem(
                1L, "01", 9, "개강 총회", "19:00", "행사", Instant.now()
        );
        DashboardPhotoItem photo = new DashboardPhotoItem(10L, "사진", "/url.jpg");
        AdminDashboardResponse response = new AdminDashboardResponse(
                orgId, "동방", stats, List.of(schedule), List.of(photo)
        );

        given(dashboardQueryService.getAdminDashboard(eq(orgId), eq(1L)))
                .willReturn(response);

        mvc.perform(get("/api/v1/organizations/{organizationId}/dashboard/admin", orgId)
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.organizationId").value(1))
                .andExpect(jsonPath("$.result.organizationName").value("동방"))
                .andExpect(jsonPath("$.result.stats.activeMemberCount").value(64))
                .andExpect(jsonPath("$.result.stats.attendanceRate").value(82.0))
                .andExpect(jsonPath("$.result.upcomingSchedules[0].title").value("개강 총회"))
                .andExpect(jsonPath("$.result.recentPhotos[0].photoId").value(10));
    }

    @Test
    @DisplayName("일반 회원 대시보드 조회 API 호출 성공")
    void getMemberDashboard() throws Exception {
        MemberPersonalStatsResponse myStats = new MemberPersonalStatsResponse(85.0, 6, 0);
        DashboardStatsResponse stats = new DashboardStatsResponse(64L, 8, 82.0, 91.0);
        MemberDashboardResponse response = new MemberDashboardResponse(
                orgId, "동방", myStats, stats, List.of(), List.of()
        );

        given(dashboardQueryService.getMemberDashboard(eq(orgId), eq(1L)))
                .willReturn(response);

        mvc.perform(get("/api/v1/organizations/{organizationId}/dashboard/member", orgId)
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.organizationId").value(1))
                .andExpect(jsonPath("$.result.myStats.attendanceRate").value(85.0))
                .andExpect(jsonPath("$.result.myStats.attendedEventCount").value(6))
                .andExpect(jsonPath("$.result.myStats.unpaidFeeCount").value(0))
                .andExpect(jsonPath("$.result.stats.activeMemberCount").value(64));
    }
}
