package com.dongbang.dashboard.presentation;

import com.dongbang.dashboard.application.DashboardQueryService;
import com.dongbang.dashboard.presentation.dto.AdminDashboardResponse;
import com.dongbang.dashboard.presentation.dto.MemberDashboardResponse;
import com.dongbang.global.response.ApiResponse;
import com.dongbang.global.response.code.GeneralSuccessCode;
import com.dongbang.global.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardQueryService dashboardQueryService;

    // 1. 운영진 대시보드 조회
    @GetMapping("/api/v1/organizations/{organizationId}/dashboard/admin")
    public ApiResponse<AdminDashboardResponse> getAdminDashboard(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId
    ) {
        AdminDashboardResponse response = dashboardQueryService.getAdminDashboard(organizationId, userId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }

    // 2. 일반 회원 대시보드 조회
    @GetMapping("/api/v1/organizations/{organizationId}/dashboard/member")
    public ApiResponse<MemberDashboardResponse> getMemberDashboard(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId
    ) {
        MemberDashboardResponse response = dashboardQueryService.getMemberDashboard(organizationId, userId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }
}
