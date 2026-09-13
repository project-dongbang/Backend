package com.dongbang.organization.presentation.dto.response;

import java.util.List;

public record MemberListResponse(
        List<MemberItemResponse> members
) {
}
