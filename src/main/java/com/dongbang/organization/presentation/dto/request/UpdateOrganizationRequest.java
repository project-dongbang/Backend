package com.dongbang.organization.presentation.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(max = 100, message = "동아리 이름은 100자 이하이어야 합니다.")
        String name,

        @Size(max = 1000, message = "동아리 설명은 1000자 이하이어야 합니다.")
        String description,

        @Size(max = 500, message = "로고 URL은 500자 이하이어야 합니다.")
        String logoUrl
) {
}
