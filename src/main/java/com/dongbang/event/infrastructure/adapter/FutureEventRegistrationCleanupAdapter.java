package com.dongbang.event.infrastructure.adapter;

import com.dongbang.event.domain.EventStatus;
import com.dongbang.event.domain.repository.EventParticipantRepository;
import com.dongbang.event.domain.repository.EventRepository;
import com.dongbang.event.application.port.EventActivityPort;
import com.dongbang.organization.application.port.FutureEventRegistrationCleanupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FutureEventRegistrationCleanupAdapter implements FutureEventRegistrationCleanupPort {
    private final EventParticipantRepository participants;
    private final EventRepository events;
    private final EventActivityPort activity;

    @Override
    public void cancelFutureRegistrations(Long membershipId, Long organizationId, Instant withdrawnAt) {
        for (var registration : participants.findByMembershipId(membershipId)) {
            var event = events.findForUpdate(registration.getEventId(), organizationId).orElse(null);
            if (event == null || event.getStatus() != EventStatus.SCHEDULED
                    || !event.getStartsAt().isAfter(withdrawnAt)) {
                continue;
            }
            registration.cancel(withdrawnAt);
            event.participantsChanged();
            activity.synchronizeParticipants(event.getId(), List.of(), List.of(membershipId));
        }
    }
}
