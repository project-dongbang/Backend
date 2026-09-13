package com.dongbang.organization.presentation.dto.response;

public record CreateOrganizationResponse(
        Long organizationId,
        String slug
) {
}
