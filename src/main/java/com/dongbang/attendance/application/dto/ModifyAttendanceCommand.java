package com.dongbang.attendance.application.dto;

import com.dongbang.attendance.domain.AttendanceStatus;

public record ModifyAttendanceCommand(AttendanceStatus status, String reason, long version) {}
