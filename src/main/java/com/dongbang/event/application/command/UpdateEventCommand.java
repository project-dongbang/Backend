package com.dongbang.event.application.command;

import com.dongbang.event.domain.Event;
import com.dongbang.event.domain.EventDetails;

import java.time.Instant;
import java.util.Set;

// HTTP 형식과 독립적인 수정 값 및 입력 여부
public record UpdateEventCommand(
        Set<String> supplied,
        String title,
        String description,
        String location,
        Instant startsAt,
        Instant endsAt,
        Integer capacity,
        Instant registrationDeadline
) {
    public UpdateEventCommand {
        supplied = Set.copyOf(supplied);
    }

    public boolean isValidPatch() {
        return !supplied.isEmpty()
                && Set.of("title", "description", "location", "startsAt", "endsAt",
                        "capacity", "registrationDeadline").containsAll(supplied)
                && (!supplied.contains("title") || (title != null && !title.isBlank()))
                && (!supplied.contains("location") || (location != null && !location.isBlank()))
                && (!supplied.contains("startsAt") || startsAt != null)
                && (!supplied.contains("endsAt") || endsAt != null);
    }

    public EventDetails merge(Event event) {
        // 필드 생략 시 기존 값 유지, 명시적 null은 설명 삭제 등 초기화 요청
        return new EventDetails(
                supplied.contains("title") ? title : event.getTitle(),
                supplied.contains("description") ? description : event.getDescription(),
                supplied.contains("location") ? location : event.getLocation(),
                supplied.contains("startsAt") ? startsAt : event.getStartsAt(),
                supplied.contains("endsAt") ? endsAt : event.getEndsAt(),
                supplied.contains("capacity") ? capacity : event.getCapacity(),
                supplied.contains("registrationDeadline") ? registrationDeadline : event.getRegistrationDeadline()
        );
    }

    public boolean changesTime(Event event) {
        EventDetails next = merge(event);
        Instant deadline = next.registrationDeadline() == null ? next.startsAt() : next.registrationDeadline();
        return !java.util.Objects.equals(next.startsAt(), event.getStartsAt())
                || !java.util.Objects.equals(next.endsAt(), event.getEndsAt())
                || !java.util.Objects.equals(deadline, event.getRegistrationDeadline());
    }
}
