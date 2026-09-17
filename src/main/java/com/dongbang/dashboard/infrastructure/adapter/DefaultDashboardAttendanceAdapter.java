package com.dongbang.dashboard.infrastructure.adapter;

import com.dongbang.dashboard.application.port.DashboardAttendancePort;

public class DefaultDashboardAttendanceAdapter implements DashboardAttendancePort {

    @Override
    public double getOrganizationAttendanceRate(Long organizationId) {
        return 0.0;
    }

    @Override
    public double getMemberAttendanceRate(Long organizationId, Long userId) {
        return 0.0;
    }

    @Override
    public int getMemberAttendanceCount(Long organizationId, Long userId) {
        return 0;
    }
}
