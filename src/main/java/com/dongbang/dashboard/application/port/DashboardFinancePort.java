package com.dongbang.dashboard.application.port;

public interface DashboardFinancePort {
    double getFeePaymentRate(Long organizationId);
    int getMemberUnpaidFeeCount(Long organizationId, Long userId);
}
