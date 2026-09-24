package com.dongbang.event.application;

import com.dongbang.event.application.result.ParticipantCandidatesResult;
import com.dongbang.event.domain.EventParticipant;
import com.dongbang.event.domain.EventType;
import com.dongbang.event.domain.EventStatus;
import com.dongbang.event.domain.ParticipantAction;
import com.dongbang.event.domain.repository.EventParticipantRepository;
import com.dongbang.event.domain.repository.EventRepository;
import com.dongbang.event.exception.EventErrorCode;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.organization.application.facade.ParticipantMemberSummary;
import com.dongbang.user.application.facade.UserAccountFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantCandidateService {
    private final EventAccessService accessService;
    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final MembershipAccessFacade memberships;
    private final UserAccountFacade users;

    public ParticipantCandidatesResult candidates(Long organizationId, Long userId, Long eventId,
                                                 String keyword, int page, int size) {
        accessService.requireStaff(organizationId, userId);
        if (page < 0 || size < 1 || size > 100) {
            throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        }
        // 참가 상태와 버전의 일관된 조회
        var event = eventRepository.findForUpdate(eventId, organizationId)
                .orElseThrow(() -> new GeneralException(EventErrorCode.EVENT_NOT_FOUND));
        if (event.getType() != EventType.EVENT) throw new GeneralException(EventErrorCode.EVENT_ONLY);
        if (event.getStatus() == EventStatus.CANCELED) throw new GeneralException(EventErrorCode.EVENT_CANCELED);
        var participating = participantRepository.findByEventIdOrderByRegisteredAtAscIdAsc(eventId).stream()
                .map(EventParticipant::getMembershipId).collect(Collectors.toSet());
        String search = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        var candidates = memberships.getParticipantMembers(organizationId).stream()
                .filter(m -> m.active() || participating.contains(m.membershipId()))
                .filter(m -> m.memberName().toLowerCase(Locale.ROOT).contains(search)
                        || m.studentNumber().contains(search))
                .sorted(Comparator.comparing(ParticipantMemberSummary::membershipId))
                .toList();
        var selected = candidates.stream().skip((long) page * size).limit(size).toList();
        var departments = users.getDepartments(selected.stream().map(ParticipantMemberSummary::userId)
                .filter(Objects::nonNull).distinct().toList());
        var members = selected.stream().map(m -> new ParticipantCandidatesResult.Member(
                m.membershipId(), m.memberName(), m.studentNumber(),
                m.userId() == null ? null : departments.get(m.userId()),
                participating.contains(m.membershipId()),
                List.of(participating.contains(m.membershipId()) ? ParticipantAction.REMOVE : ParticipantAction.ADD)))
                .toList();
        int totalPages = (int) ((candidates.size() + (long) size - 1) / size);
        return new ParticipantCandidatesResult(eventId, event.getCapacity(), participating.size(),
                event.getParticipantVersion(), members, page, size, candidates.size(), totalPages,
                (long) page + 1 < totalPages);
    }
}
