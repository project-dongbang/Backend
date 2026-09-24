package com.dongbang.attendance.presentation.dto.request;

import com.dongbang.attendance.application.dto.ModifyAttendanceCommand;
import com.dongbang.attendance.domain.AttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ModifyAttendanceRequest(
        @NotNull AttendanceStatus status,
        @NotBlank @Size(max = 500) String reason,
        @PositiveOrZero long version
) {
    public ModifyAttendanceCommand toCommand() {
        return new ModifyAttendanceCommand(status, reason.trim(), version);
    }
}
