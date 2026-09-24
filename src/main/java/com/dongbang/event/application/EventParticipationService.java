package com.dongbang.event.application;

import com.dongbang.event.domain.*;
import com.dongbang.event.application.command.ChangeParticipantsCommand;
import com.dongbang.event.application.port.EventActivityPort;
import com.dongbang.event.application.result.ChangeParticipantsResult;
import com.dongbang.event.application.result.EventApplicationResult;
import com.dongbang.finance.application.facade.AuditLogFacade;
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
import java.util.HashSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EventParticipationService {
    private final EventRepository events;
    private final EventParticipantRepository participants;
    private final EventAccessService access;
    private final MembershipAccessFacade memberships;
    private final Clock clock;
    private final AuditLogFacade auditLog;
    private final EventActivityPort activityPort;

    public EventApplicationResult apply(Long organizationId, Long userId, Long eventId) {
        access.requireMember(organizationId, userId);
        Event event = locked(organizationId, eventId);
        requireOpen(event);
        Long membershipId = ownMembership(organizationId, userId);
        add(event, membershipId, EventErrorCode.REGISTRATION_FULL);
        activityPort.synchronizeParticipants(eventId, List.of(membershipId), List.of());
        audit(event, membershipId, "EVENT_APPLY", null, membershipId.toString());
        return new EventApplicationResult(eventId, membershipId, true,
                participants.countByEventId(eventId), event.getParticipantVersion());
    }

    public void withdraw(Long organizationId, Long userId, Long eventId) {
        access.requireMember(organizationId, userId);
        Event event = locked(organizationId, eventId);
        requireOpen(event);
        Long membershipId = ownMembership(organizationId, userId);
        remove(event, membershipId);
        activityPort.synchronizeParticipants(eventId, List.of(), List.of(membershipId));
        audit(event, membershipId, "EVENT_WITHDRAW", membershipId.toString(), null);
    }

    public void closeRegistration(Long organizationId, Long userId, Long eventId) {
        Long actorId = access.requireStaff(organizationId, userId);
        Event event = locked(organizationId, eventId);
        if (event.getRegistrationClosedAt() == null) {
            requireOpen(event);
            event.closeRegistration(clock.instant());
            audit(event, actorId, "EVENT_REGISTRATION_CLOSE",
                    "\"" + event.getRegistrationDeadline() + "\"", "\"" + event.getRegistrationClosedAt() + "\"");
        }
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
    }

    private void add(Event event, Long membershipId, EventErrorCode capacityError) {
        if (participants.findByEventIdAndMembershipId(event.getId(), membershipId).isPresent())
            throw new GeneralException(EventErrorCode.ALREADY_PARTICIPATING);
        if (event.getCapacity() != null && participants.countByEventId(event.getId()) >= event.getCapacity())
            throw new GeneralException(capacityError);
        register(event, membershipId);
        event.participantsChanged();
    }

    private void remove(Event event, Long membershipId) {
        participants.findByEventIdAndMembershipId(event.getId(), membershipId)
                .orElseThrow(() -> new GeneralException(EventErrorCode.PARTICIPANT_NOT_FOUND))
                .cancel(clock.instant());
        event.participantsChanged();
    }

    public ChangeParticipantsResult changeParticipants(Long organizationId, Long userId, Long eventId,
                                                       ChangeParticipantsCommand command) {
        Long actorId = access.requireStaff(organizationId, userId);
        Event event = locked(organizationId, eventId);
        requireEditable(event, command.participantVersion());
        if (command.changes() == null || command.changes().isEmpty()) {
            throw new GeneralException(EventErrorCode.INVALID_PARTICIPANT_CHANGE);
        }
        var current = participants.findByEventIdOrderByRegisteredAtAscIdAsc(eventId).stream()
                .collect(Collectors.toMap(EventParticipant::getMembershipId, p -> p));
        var seen = new HashSet<Long>();
        int added = 0;
        int removed = 0;
        // 모든 대상과 최종 정원 검증 후 일괄 반영
        for (var change : command.changes()) {
            if (change == null || change.membershipId() == null || change.membershipId() <= 0
                    || change.action() == null || !seen.add(change.membershipId())) {
                throw new GeneralException(EventErrorCode.INVALID_PARTICIPANT_CHANGE);
            }
            if (change.action() == ParticipantAction.ADD) {
                memberships.getMembershipSummaryById(change.membershipId())
                        .filter(m -> organizationId.equals(m.organizationId()) && m.status() == MembershipStatus.ACTIVE)
                        .orElseThrow(() -> new GeneralException(EventErrorCode.INVALID_PARTICIPANT_CHANGE));
                if (current.containsKey(change.membershipId())) {
                    throw new GeneralException(EventErrorCode.VERSION_CONFLICT);
                }
                added++;
            } else {
                if (!current.containsKey(change.membershipId())) {
                    throw new GeneralException(EventErrorCode.VERSION_CONFLICT);
                }
                removed++;
            }
        }
        int count = current.size() + added - removed;
        if (event.getCapacity() != null && count > event.getCapacity()) {
            throw new GeneralException(EventErrorCode.CAPACITY_EXCEEDED);
        }
        for (var change : command.changes()) {
            if (change.action() == ParticipantAction.ADD) {
                register(event, change.membershipId());
            } else {
                current.get(change.membershipId()).cancel(clock.instant());
            }
        }
        event.participantsChanged();
        var after = new TreeSet<>(current.keySet());
        command.changes().forEach(c -> {
            if (c.action() == ParticipantAction.ADD) after.add(c.membershipId());
            else after.remove(c.membershipId());
        });
        audit(event, actorId, "EVENT_PARTICIPANTS_CHANGE",
                new TreeSet<>(current.keySet()).toString(), after.toString());
        var addedIds = command.changes().stream().filter(c -> c.action() == ParticipantAction.ADD)
                .map(c -> c.membershipId()).toList();
        var removedIds = command.changes().stream().filter(c -> c.action() == ParticipantAction.REMOVE)
                .map(c -> c.membershipId()).toList();
        activityPort.synchronizeParticipants(eventId, addedIds, removedIds);
        return new ChangeParticipantsResult(eventId, added, removed, count,
                event.getCapacity(), event.getParticipantVersion());
    }

    private void register(Event event, Long membershipId) {
        var existing = participants.findRegistration(event.getId(), membershipId);
        if (existing.isPresent()) existing.get().register(clock.instant());
        else participants.save(new EventParticipant(event.getId(), membershipId, clock.instant()));
    }

    private void audit(Event event, Long actorId, String action, String before, String after) {
        auditLog.record(event.getOrganizationId(), actorId, action, "EVENT", event.getId(), before, after);
    }

}
