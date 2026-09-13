package com.dongbang.organization.presentation.dto.response;

import java.util.List;

public record MyOrganizationResponse(
        List<MyOrganizationItemResponse> organizations
) {
}
