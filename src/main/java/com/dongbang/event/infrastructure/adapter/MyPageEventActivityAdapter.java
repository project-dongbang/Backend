package com.dongbang.event.infrastructure.adapter;

import com.dongbang.event.domain.EventStatus;
import com.dongbang.event.domain.EventType;
import com.dongbang.event.domain.repository.EventParticipantRepository;
import com.dongbang.event.domain.repository.EventRepository;
import com.dongbang.mypage.application.port.MyActivityEventPort;
import com.dongbang.mypage.application.port.RegisteredEventActivity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MyPageEventActivityAdapter implements MyActivityEventPort {

    private final EventParticipantRepository participantRepository;
    private final EventRepository eventRepository;

    @Override
    public List<RegisteredEventActivity> findRegisteredEvents(Long organizationId, Long membershipId) {
        return participantRepository.findByMembershipId(membershipId).stream()
                .map(registration -> eventRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(
                        registration.getEventId(), organizationId).orElse(null))
                .filter(event -> event != null
                        && event.getType() == EventType.EVENT
                        && event.getStatus() == EventStatus.SCHEDULED)
                .map(event -> new RegisteredEventActivity(event.getId(), event.getTitle(), event.getStartsAt()))
                .sorted(Comparator.comparing(RegisteredEventActivity::startsAt)
                        .thenComparing(RegisteredEventActivity::eventId))
                .toList();
    }
}
