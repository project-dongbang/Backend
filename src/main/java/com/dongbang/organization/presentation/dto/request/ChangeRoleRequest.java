package com.dongbang.organization.presentation.dto.request;

import com.dongbang.organization.domain.MembershipRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull(message = "변경할 권한은 필수입니다.")
        MembershipRole role
) {
}
