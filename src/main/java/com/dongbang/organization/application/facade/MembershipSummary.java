package com.dongbang.organization.application.facade;

import com.dongbang.organization.domain.MembershipRole;
import com.dongbang.organization.domain.MembershipStatus;

public record MembershipSummary(
        Long membershipId,
        Long organizationId,
        Long userId,
        String memberName,
        MembershipRole role,
        MembershipStatus status
) {
}
