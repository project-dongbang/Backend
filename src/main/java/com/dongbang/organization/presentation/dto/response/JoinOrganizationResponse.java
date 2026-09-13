package com.dongbang.organization.presentation.dto.response;

import com.dongbang.organization.domain.MembershipRole;

public record JoinOrganizationResponse(
        Long organizationId,
        Long membershipId,
        MembershipRole role
) {
}
