package com.dongbang.event.application;

import com.dongbang.event.domain.*;
import com.dongbang.event.domain.repository.*;
import com.dongbang.event.exception.EventErrorCode;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.organization.application.facade.MembershipSummary;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.organization.exception.OrganizationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EventParticipationService {
    private final EventRepository events;
    private final EventParticipantRepository participants;
    private final EventAccessService access;
    private final MembershipAccessFacade memberships;
    private final Clock clock;

    public void apply(Long organizationId, Long userId, Long eventId) {
        access.requireMember(organizationId, userId);
        Event event = locked(organizationId, eventId);
        requireOpen(event);
        add(event, ownMembership(organizationId, userId));
    }

    public void withdraw(Long organizationId, Long userId, Long eventId) {
        access.requireMember(organizationId, userId);
        Event event = locked(organizationId, eventId);
        requireOpen(event);
        remove(event, ownMembership(organizationId, userId));
    }

    public void addParticipant(Long organizationId, Long userId, Long eventId, Long membershipId, long version) {
        access.requireStaff(organizationId, userId);
        Event event = locked(organizationId, eventId);
        requireEditable(event, version);
        memberships.getMembershipSummaryById(membershipId)
                .filter(m -> organizationId.equals(m.organizationId()) && m.status() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.MEMBER_REQUIRED));
        add(event, membershipId);
    }

    public void removeParticipant(Long organizationId, Long userId, Long eventId, Long membershipId, long version) {
        access.requireStaff(organizationId, userId);
        Event event = locked(organizationId, eventId);
        requireEditable(event, version);
        remove(event, membershipId);
    }

    public void closeRegistration(Long organizationId, Long userId, Long eventId) {
        access.requireStaff(organizationId, userId);
        Event event = locked(organizationId, eventId);
        if (event.getRegistrationClosedAt() == null) event.closeRegistration(clock.instant());
    }

    public void cancel(Long organizationId, Long userId, Long eventId) {
        access.requireStaff(organizationId, userId);
        Event event = events.findForUpdate(eventId, organizationId)
                .orElseThrow(() -> new GeneralException(EventErrorCode.EVENT_NOT_FOUND));
        if (event.getStatus() != EventStatus.CANCELED) event.cancel(clock.instant());
    }

    @Transactional
    public ParticipantList list(Long organizationId, Long userId, Long eventId) {
        access.requireStaff(organizationId, userId);
        // 같은 행사 행을 잠가 목록과 버전을 일관된 스냅샷으로 반환한다.
        Event event = events.findForUpdate(eventId, organizationId)
                .orElseThrow(() -> new GeneralException(EventErrorCode.EVENT_NOT_FOUND));
        if (event.getType() != EventType.EVENT) throw new GeneralException(EventErrorCode.EVENT_ONLY);
        var rows = participants.findByEventIdOrderByRegisteredAtAscIdAsc(eventId);
        var names = memberships.getMembershipSummariesByIds(rows.stream().map(EventParticipant::getMembershipId).toList());
        return new ParticipantList(event.getParticipantVersion(), rows.stream().map(p -> {
            var member = names.get(p.getMembershipId());
            return new ParticipantItem(p.getMembershipId(), member == null ? null : member.memberName(), p.getRegisteredAt());
        }).toList());
    }

    private Event locked(Long organizationId, Long eventId) {
        Event event = events.findForUpdate(eventId, organizationId)
                .orElseThrow(() -> new GeneralException(EventErrorCode.EVENT_NOT_FOUND));
        if (event.getType() != EventType.EVENT) throw new GeneralException(EventErrorCode.EVENT_ONLY);
        if (event.getStatus() == EventStatus.CANCELED) throw new GeneralException(EventErrorCode.EVENT_CANCELED);
        return event;
    }

    private Long ownMembership(Long organizationId, Long userId) {
        return memberships.getMembershipSummary(organizationId, userId).map(MembershipSummary::membershipId)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.MEMBER_REQUIRED));
    }

    private void requireOpen(Event event) {
        Instant now = clock.instant();
        if (event.getRegistrationClosedAt() != null || !now.isBefore(event.getRegistrationDeadline())
                || (event.getRegistrationOpensAt() != null && now.isBefore(event.getRegistrationOpensAt())))
            throw new GeneralException(EventErrorCode.REGISTRATION_CLOSED);
    }

    private void requireEditable(Event event, long version) {
        if (event.getParticipantVersion() != version) throw new GeneralException(EventErrorCode.VERSION_CONFLICT);
        if (!clock.instant().isBefore(event.getStartsAt())) throw new GeneralException(EventErrorCode.REGISTRATION_CLOSED);
    }

    private void add(Event event, Long membershipId) {
        if (participants.findByEventIdAndMembershipId(event.getId(), membershipId).isPresent())
            throw new GeneralException(EventErrorCode.ALREADY_PARTICIPATING);
        if (event.getCapacity() != null && participants.countByEventId(event.getId()) >= event.getCapacity())
            throw new GeneralException(EventErrorCode.CAPACITY_EXCEEDED);
        participants.save(new EventParticipant(event.getId(), membershipId, clock.instant()));
        event.participantsChanged();
    }

    private void remove(Event event, Long membershipId) {
        participants.delete(participants.findByEventIdAndMembershipId(event.getId(), membershipId)
                .orElseThrow(() -> new GeneralException(EventErrorCode.PARTICIPANT_NOT_FOUND)));
        event.participantsChanged();
    }

    public record ParticipantItem(Long membershipId, String memberName, Instant registeredAt) {}
    public record ParticipantList(long participantVersion, List<ParticipantItem> participants) {}
}
