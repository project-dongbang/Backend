package com.dongbang.event.presentation.dto.request;

import com.dongbang.event.domain.EventDetails;
import com.dongbang.event.application.command.CreateEventCommand;
import com.dongbang.event.domain.EventType;
import jakarta.validation.constraints.*;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record CreateEventRequest(
        @Schema(description = "일정 종류. SCHEDULE은 정원·신청 마감을 설정할 수 없습니다.", example = "EVENT")
        @NotNull(message = "일정 종류는 필수입니다.") EventType type,
        @Schema(description = "제목 (최대 200자)", example = "신입 부원 환영 행사")
        @NotBlank(message = "제목은 필수입니다.") @Size(max = 200) String title,
        @Schema(description = "시작 시각. UTC 또는 오프셋 포함 ISO 8601", example = "2026-09-20T09:00:00+09:00")
        @NotNull(message = "시작 시각은 필수입니다.") Instant startsAt,
        @Schema(description = "종료 시각. 시작보다 이후", example = "2026-09-20T11:00:00+09:00")
        @NotNull(message = "종료 시각은 필수입니다.") Instant endsAt,
        @Schema(description = "장소 (최대 255자)", example = "학생회관 201호")
        @NotBlank(message = "장소는 필수입니다.") @Size(max = 255) String location,
        @Schema(description = "상세 설명 (최대 5000자). null로 삭제 가능", example = "신입 부원 환영 행사입니다.")
        @Size(max = 5000) String description,
        @Schema(description = "행사 정원. 양수이며 null은 정원 제한 없음", example = "20")
        @Positive(message = "정원은 1 이상이어야 합니다.") Integer capacity,
        @Schema(description = "행사 신청 마감. 시작 시각 이하이며 null이면 시작 시각 사용", example = "2026-09-19T18:00:00+09:00")
        Instant registrationDeadline
) {
    public CreateEventCommand toCommand() {
        return new CreateEventCommand(type,
                new EventDetails(title, description, location, startsAt, endsAt, capacity, registrationDeadline));
    }
}
