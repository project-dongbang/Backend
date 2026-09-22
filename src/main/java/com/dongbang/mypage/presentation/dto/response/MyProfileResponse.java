package com.dongbang.mypage.presentation.dto.response;

import com.dongbang.auth.domain.OAuthProvider;

import java.util.List;

public record MyProfileResponse(
        Long userId,
        String name,
        String studentNumber,
        String department,
        String email,
        String profileImageUrl,
        List<OAuthProvider> oauthProviders,
        CurrentMembershipResponse currentMembership
) {
}
