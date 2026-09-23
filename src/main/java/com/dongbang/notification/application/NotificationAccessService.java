package com.dongbang.notification.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.organization.domain.OrganizationStatus;
import com.dongbang.organization.domain.repository.MembershipRepository;
import com.dongbang.organization.domain.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationAccessService {
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;

    public void requireMember(Long organizationId, Long userId) {
        organizationRepository.findById(organizationId)
                .filter(organization -> organization.getStatus() == OrganizationStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
        membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN));
    }
}
