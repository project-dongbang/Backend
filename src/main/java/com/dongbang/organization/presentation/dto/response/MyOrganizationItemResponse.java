package com.dongbang.organization.presentation.dto.response;

import com.dongbang.organization.domain.MembershipRole;

import java.time.Instant;

public record MyOrganizationItemResponse(
        Long organizationId,
        String name,
        String slug,
        String logoUrl,
        MembershipRole myRole,
        Instant joinedAt
) {
}
