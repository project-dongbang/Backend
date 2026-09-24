package com.dongbang.event.application;

import com.dongbang.event.application.port.EventActivity;
import com.dongbang.event.application.port.EventActivityPort;
import com.dongbang.event.domain.Event;
import com.dongbang.event.domain.EventDetails;
import com.dongbang.event.domain.repository.EventRepository;
import com.dongbang.event.exception.EventErrorCode;
import com.dongbang.event.application.command.CreateEventCommand;
import com.dongbang.event.application.command.UpdateEventCommand;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class EventCommandService {

    private final EventRepository eventRepository;
    private final EventAccessService accessService;
    private final EventActivityPort activityPort;
    private final Clock clock;

    public Long create(Long organizationId, Long userId, CreateEventCommand request) {
        // 작성자 정보에 동아리 멤버십 ID 사용
        Long membershipId = accessService.requireStaff(organizationId, userId);
        Event event = Event.builder()
                .organizationId(organizationId)
                .createdByMembershipId(membershipId)
                .type(request.type())
                .details(request.details())
                .build();
        Event saved = eventRepository.save(event);
        // TODO(notification): 커밋 성공 후 SCHEDULE_CREATED 알림 연계 및 중복 방지 키 적용.
        return saved.getId();
    }

    public void update(Long organizationId, Long userId, Long eventId, UpdateEventCommand request) {
        accessService.requireStaff(organizationId, userId);
        if (!request.isValidPatch()) {
            throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        }
        Event event = findForUpdate(organizationId, eventId);
        if (event.getStatus() == com.dongbang.event.domain.EventStatus.CANCELED) {
            throw new GeneralException(EventErrorCode.EVENT_CANCELED);
        }
        EventActivity activity = activityPort.getActivity(organizationId, eventId, userId);
        EventDetails next = request.merge(event);
        // 현재 참가자 수 미만으로 정원 축소 불가
        if (next.capacity() != null && next.capacity() < activity.participantCount()) {
            throw new GeneralException(EventErrorCode.CAPACITY_EXCEEDED);
        }
        // TODO(policy): 출석 시작 후 시각 변경 제한 정책 최종 합의 필요
        if (activity.hasAttendanceStarted() && request.changesTime(event)) {
            throw new GeneralException(EventErrorCode.ATTENDANCE_ALREADY_STARTED);
        }
        var deadline = next.registrationDeadline() == null ? next.startsAt() : next.registrationDeadline();
        if (activity.registrationClosedEarly()
                && !Objects.equals(deadline, event.getRegistrationDeadline())) {
            throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR,
                    "조기 마감된 신청의 마감 시각은 변경할 수 없습니다.");
        }
        event.revise(next);
    }

    public void delete(Long organizationId, Long userId, Long eventId) {
        accessService.requireStaff(organizationId, userId);
        Event event = findForUpdate(organizationId, eventId);
        // TODO(policy): 출석 시작 후 삭제 제한 정책 최종 합의 필요
        if (activityPort.getActivity(organizationId, eventId, userId).hasAttendanceStarted()) {
            throw new GeneralException(EventErrorCode.ATTENDANCE_ALREADY_STARTED);
        }
        event.delete(clock.instant());
    }

    private Event findForUpdate(Long organizationId, Long eventId) {
        return eventRepository.findForUpdate(eventId, organizationId)
                .orElseThrow(() -> new GeneralException(EventErrorCode.EVENT_NOT_FOUND));
    }
}
