package com.dongbang.finance.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.organization.domain.OrganizationStatus;
import com.dongbang.organization.domain.repository.MembershipRepository;
import com.dongbang.organization.domain.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FinanceAccessService {
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;

    public Membership requireMember(Long organizationId, Long userId) {
        organizationRepository.findById(organizationId)
                .filter(org -> org.getStatus() == OrganizationStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
        return membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN));
    }

    public Membership requireStaff(Long organizationId, Long userId) {
        Membership membership = requireMember(organizationId, userId);
        if (!membership.getRole().isStaff()) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN);
        }
        return membership;
    }
}
