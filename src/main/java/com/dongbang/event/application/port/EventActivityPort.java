package com.dongbang.event.application.port;

import java.util.List;

public interface EventActivityPort {
    EventActivity getActivity(Long organizationId, Long eventId, Long userId);
    void synchronizeParticipants(Long eventId, List<Long> added, List<Long> removed);
}
