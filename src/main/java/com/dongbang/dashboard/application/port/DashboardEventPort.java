package com.dongbang.dashboard.application.port;

import java.util.List;

public interface DashboardEventPort {
    int countThisMonthEvents(Long organizationId);
    List<DashboardScheduleItem> getUpcomingSchedules(Long organizationId, int limit);
}
