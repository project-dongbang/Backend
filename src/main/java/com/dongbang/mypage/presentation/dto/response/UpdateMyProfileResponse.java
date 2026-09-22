package com.dongbang.mypage.presentation.dto.response;

import java.time.Instant;

public record UpdateMyProfileResponse(
        Long userId,
        String name,
        String studentNumber,
        String department,
        String email,
        Instant updatedAt
) {
}
