package com.dongbang.mypage.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.mypage.application.port.MyActivityEventPort;
import com.dongbang.mypage.application.port.MyActivityFeePort;
import com.dongbang.mypage.presentation.dto.response.MyActivitiesResponse;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.organization.domain.OrganizationStatus;
import com.dongbang.organization.domain.repository.MembershipRepository;
import com.dongbang.organization.domain.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageActivityService {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final MyActivityEventPort eventPort;
    private final MyActivityFeePort feePort;

    public MyActivitiesResponse getMyActivities(Long userId, Long organizationId) {
        organizationRepository.findById(organizationId)
                .filter(organization -> organization.getStatus() == OrganizationStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
        var membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .filter(item -> item.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN));

        var events = eventPort.findRegisteredEvents(organizationId, membership.getId()).stream()
                .map(event -> new MyActivitiesResponse.RegisteredEvent(
                        event.eventId(), event.title(), event.startsAt(), "REGISTERED"))
                .toList();
        return new MyActivitiesResponse(
                new MyActivitiesResponse.Summary(events.size(), feePort.getFeeStatus(membership.getId())),
                events
        );
    }
}
