package com.dongbang.event.domain;

import java.time.Instant;

public record EventDetails(
        String title,
        String description,
        String location,
        Instant startsAt,
        Instant endsAt,
        Integer capacity,
        Instant registrationDeadline
) {
}
