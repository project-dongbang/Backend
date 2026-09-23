package com.dongbang.event.application.port;

public record EventActivity(
        int participantCount,
        boolean participating,
        long participantVersion,
        String attendanceSessionStatus,
        boolean registrationClosedEarly
) {
    public boolean hasAttendanceStarted() {
        return !"NOT_STARTED".equals(attendanceSessionStatus);
    }
}
