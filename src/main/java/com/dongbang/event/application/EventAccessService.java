package com.dongbang.event.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.exception.OrganizationErrorCode;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.organization.application.facade.MembershipSummary;
import com.dongbang.organization.application.facade.OrganizationAccessFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventAccessService {

    private final MembershipAccessFacade membershipAccessFacade;
    private final OrganizationAccessFacade organizationAccessFacade;

    public void requireMember(Long organizationId, Long userId) {
        // 로그인 여부와 별도로 동아리 활동 회원 권한 확인
        if (!membershipAccessFacade.isActiveMember(organizationId, userId)) {
            throw new GeneralException(OrganizationErrorCode.MEMBER_REQUIRED);
        }
        organizationAccessFacade.requireActiveOrganization(organizationId);
    }

    public Long requireStaff(Long organizationId, Long userId) {
        if (!membershipAccessFacade.isStaff(organizationId, userId)) {
            throw new GeneralException(OrganizationErrorCode.STAFF_REQUIRED);
        }
        organizationAccessFacade.requireActiveOrganization(organizationId);
        return membershipAccessFacade.getMembershipSummary(organizationId, userId)
                .map(MembershipSummary::membershipId)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.STAFF_REQUIRED));
    }
}
