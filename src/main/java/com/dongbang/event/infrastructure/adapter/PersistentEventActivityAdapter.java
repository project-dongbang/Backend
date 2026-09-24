package com.dongbang.event.infrastructure.adapter;

import com.dongbang.attendance.application.facade.AttendanceIntegrationFacade;
import com.dongbang.event.application.port.*;
import com.dongbang.event.domain.repository.*;
import com.dongbang.event.exception.EventErrorCode;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersistentEventActivityAdapter implements EventActivityPort {
    private final EventRepository events;
    private final EventParticipantRepository participants;
    private final MembershipAccessFacade memberships;
    private final AttendanceIntegrationFacade attendance;

    @Override
    public EventActivity getActivity(Long organizationId, Long eventId, Long userId) {
        var event = events.findByIdAndOrganizationIdAndDeletedAtIsNull(eventId, organizationId)
                .orElseThrow(() -> new GeneralException(EventErrorCode.EVENT_NOT_FOUND));
        boolean participating = memberships.getMembershipSummary(organizationId, userId)
                .map(m -> participants.findByEventIdAndMembershipId(eventId, m.membershipId()).isPresent()).orElse(false);
        return new EventActivity(participants.countByEventId(eventId), participating,
                event.getParticipantVersion(), attendance.status(eventId).name(), event.getRegistrationClosedAt() != null);
    }

    @Override
    @Transactional
    public void synchronizeParticipants(Long eventId, List<Long> added, List<Long> removed) {
        attendance.synchronizeParticipants(eventId, added, removed);
    }
}
