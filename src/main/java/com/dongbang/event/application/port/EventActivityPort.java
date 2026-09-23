package com.dongbang.event.application.port;

public interface EventActivityPort {
    EventActivity getActivity(Long organizationId, Long eventId, Long userId);
}
