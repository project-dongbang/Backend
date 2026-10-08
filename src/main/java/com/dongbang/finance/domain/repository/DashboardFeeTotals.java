package com.dongbang.finance.domain.repository;

public record DashboardFeeTotals(long total, long paid) {
    public double rate() {
        return total == 0 ? 0.0 : Math.round(paid * 1000.0 / total) / 10.0;
    }
}
