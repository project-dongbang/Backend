package com.dongbang.event.infrastructure.adapter;

import com.dongbang.event.application.port.*;
import com.dongbang.event.domain.repository.*;
import com.dongbang.event.exception.EventErrorCode;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersistentEventActivityAdapter implements EventActivityPort {
    private final EventRepository events;
    private final EventParticipantRepository participants;
    private final MembershipAccessFacade memberships;

    @Override
    public EventActivity getActivity(Long organizationId, Long eventId, Long userId) {
        var event = events.findByIdAndOrganizationIdAndDeletedAtIsNull(eventId, organizationId)
                .orElseThrow(() -> new GeneralException(EventErrorCode.EVENT_NOT_FOUND));
        boolean participating = memberships.getMembershipSummary(organizationId, userId)
                .map(m -> participants.findByEventIdAndMembershipId(eventId, m.membershipId()).isPresent()).orElse(false);
        return new EventActivity(participants.countByEventId(eventId), participating,
                event.getParticipantVersion(), "NOT_STARTED", event.getRegistrationClosedAt() != null);
    }
}
