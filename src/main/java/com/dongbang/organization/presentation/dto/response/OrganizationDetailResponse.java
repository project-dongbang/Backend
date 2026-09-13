package com.dongbang.organization.presentation.dto.response;

import java.time.Instant;

public record OrganizationDetailResponse(
        Long organizationId,
        String name,
        String slug,
        String description,
        String logoUrl,
        long memberCount,
        Instant createdAt
) {
}
