package com.dongbang.organization.presentation.dto.response;

import com.dongbang.organization.domain.MembershipRole;
import com.dongbang.organization.domain.MembershipStatus;

import java.time.Instant;

public record MemberItemResponse(
        Long membershipId,
        Long userId,
        String memberName,
        String studentNumber,
        String generation,
        String position,
        MembershipRole role,
        MembershipStatus status,
        Instant joinedAt
) {
}
