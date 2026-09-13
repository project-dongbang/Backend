package com.dongbang.organization.presentation.dto.response;

import java.time.Instant;

public record InvitationResponse(
        String invitationToken,
        Instant expiresAt
) {
}
