package com.dongbang.dashboard.application.port;

public interface DashboardAttendancePort {
    double getOrganizationAttendanceRate(Long organizationId);
    double getMemberAttendanceRate(Long organizationId, Long userId);
    int getMemberAttendanceCount(Long organizationId, Long userId);
}
