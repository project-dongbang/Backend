package com.dongbang.dashboard.infrastructure.adapter;

import com.dongbang.dashboard.application.port.DashboardAttendancePort;
import com.dongbang.dashboard.application.port.DashboardEventPort;
import com.dongbang.dashboard.application.port.DashboardFinancePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DashboardPortConfiguration {

    @Bean
    @ConditionalOnMissingBean(DashboardEventPort.class)
    public DashboardEventPort defaultDashboardEventPort() {
        return new DefaultDashboardEventAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(DashboardAttendancePort.class)
    public DashboardAttendancePort defaultDashboardAttendancePort() {
        return new DefaultDashboardAttendanceAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(DashboardFinancePort.class)
    public DashboardFinancePort defaultDashboardFinancePort() {
        return new DefaultDashboardFinanceAdapter();
    }
}
