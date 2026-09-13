package com.dongbang.organization.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateInvitationRequest(
        @Min(value = 1, message = "만료 시간은 최소 1시간 이상이어야 합니다.")
        @Max(value = 720, message = "만료 시간은 최대 720시간(30일) 이하이어야 합니다.")
        Integer expiresInHours
) {
    public Integer getEffectiveExpiresInHours() {
        return (expiresInHours != null && expiresInHours > 0) ? expiresInHours : 24;
    }
}
