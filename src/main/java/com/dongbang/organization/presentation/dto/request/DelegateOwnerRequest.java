package com.dongbang.organization.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record DelegateOwnerRequest(
        @NotNull(message = "위임 대상 멤버 ID는 필수입니다.")
        Long targetMemberId
) {
}
