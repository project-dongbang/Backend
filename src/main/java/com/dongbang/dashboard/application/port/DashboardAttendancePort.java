package com.dongbang.dashboard.application.port;

public interface DashboardAttendancePort {
    double getOrganizationAttendanceRate(Long organizationId);
    MemberAttendanceStats getMemberAttendanceStats(Long organizationId, Long userId);

    record MemberAttendanceStats(double attendanceRate, int attendedEventCount) {}
}
