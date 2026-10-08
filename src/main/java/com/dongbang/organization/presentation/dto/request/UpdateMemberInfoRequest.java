package com.dongbang.organization.presentation.dto.request;

import com.dongbang.organization.domain.MembershipStatus;
import jakarta.validation.constraints.Size;

public record UpdateMemberInfoRequest(
        ActivityStatus status,
        @Size(max = 20) String generation,
        @Size(max = 50) String position
) {
    public enum ActivityStatus {
        ACTIVE, INACTIVE;

        public MembershipStatus toMembershipStatus() {
            return MembershipStatus.valueOf(name());
        }
    }
}
