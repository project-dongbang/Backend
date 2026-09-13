package com.dongbang.organization.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank(message = "동아리 이름은 필수입니다.")
        @Size(max = 100, message = "동아리 이름은 100자 이하이어야 합니다.")
        String name,

        @NotBlank(message = "슬러그는 필수입니다.")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "슬러그는 영문 소문자, 숫자, 하이픈(-)만 가능합니다.")
        @Size(max = 100, message = "슬러그는 100자 이하이어야 합니다.")
        String slug,

        @Size(max = 1000, message = "동아리 설명은 1000자 이하이어야 합니다.")
        String description,

        @Size(max = 500, message = "로고 URL은 500자 이하이어야 합니다.")
        String logoUrl
) {
}
