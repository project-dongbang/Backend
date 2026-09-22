package com.dongbang.mypage.presentation.dto.response;

public record CurrentMembershipResponse(
        Long organizationId,
        String role,
        String generation,
        String position
) {
}
