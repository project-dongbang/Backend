package com.dongbang.mypage.presentation.dto.response;

import com.dongbang.mypage.domain.MyFeeStatus;

import java.time.Instant;
import java.util.List;

public record MyActivitiesResponse(
        Summary summary,
        List<RegisteredEvent> registeredEvents
) {
    public record Summary(int eventRegistrationCount, MyFeeStatus feeStatus) {
    }

    public record RegisteredEvent(
            Long eventId,
            String title,
            Instant startsAt,
            String registrationStatus
    ) {
    }
}
