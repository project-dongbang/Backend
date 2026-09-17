package com.dongbang.dashboard.infrastructure.adapter;

import com.dongbang.dashboard.application.port.DashboardEventPort;
import com.dongbang.dashboard.application.port.DashboardScheduleItem;

import java.util.Collections;
import java.util.List;

public class DefaultDashboardEventAdapter implements DashboardEventPort {

    @Override
    public int countThisMonthEvents(Long organizationId) {
        return 0;
    }

    @Override
    public List<DashboardScheduleItem> getUpcomingSchedules(Long organizationId, int limit) {
        return Collections.emptyList();
    }
}
