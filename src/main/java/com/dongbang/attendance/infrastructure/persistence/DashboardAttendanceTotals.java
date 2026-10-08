package com.dongbang.attendance.infrastructure.persistence;

public record DashboardAttendanceTotals(long total, long present) {
    public double rate() {
        return total == 0 ? 0.0 : Math.round(present * 1000.0 / total) / 10.0;
    }
}
