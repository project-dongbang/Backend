package com.dongbang.dashboard.infrastructure.adapter;

import com.dongbang.dashboard.application.port.DashboardFinancePort;

public class DefaultDashboardFinanceAdapter implements DashboardFinancePort {

    @Override
    public double getFeePaymentRate(Long organizationId) {
        return 0.0;
    }

    @Override
    public int getMemberUnpaidFeeCount(Long organizationId, Long userId) {
        return 0;
    }
}
