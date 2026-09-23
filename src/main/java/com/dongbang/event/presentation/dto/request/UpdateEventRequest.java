package com.dongbang.event.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.dongbang.event.application.command.UpdateEventCommand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

// PATCH의 생략·null 구분을 위한 setter 기반 입력 추적
public class UpdateEventRequest {

    @JsonIgnore
    private final Set<String> supplied = new HashSet<>();

    @Size(max = 200)
    @Schema(description = "제목 (최대 200자) 미전달 시 유지.", example = "신입 부원 환영 행사", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String title;
    @Size(max = 5000)
    @Schema(description = "상세 설명 (최대 5000자). null로 삭제 가능 미전달 시 유지.", example = "신입 부원 환영 행사입니다.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String description;
    @Size(max = 255)
    @Schema(description = "장소 (최대 255자) 미전달 시 유지.", example = "학생회관 201호", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String location;
    @Schema(description = "시작 시각. UTC 또는 오프셋 포함 ISO 8601 미전달 시 유지.", example = "2026-09-20T09:00:00+09:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Instant startsAt;
    @Schema(description = "종료 시각. 시작보다 이후 미전달 시 유지.", example = "2026-09-20T11:00:00+09:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Instant endsAt;
    @Positive
    @Schema(description = "행사 정원. 양수이며 null은 정원 제한 없음 미전달 시 유지.", example = "20", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer capacity;
    @Schema(description = "행사 신청 마감. 시작 시각 이하이며 null이면 시작 시각 사용 미전달 시 유지.", example = "2026-09-19T18:00:00+09:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Instant registrationDeadline;

    @JsonSetter
    public void setTitle(String value) { supplied.add("title"); title = value; }

    @JsonSetter
    public void setDescription(String value) { supplied.add("description"); description = value; }

    @JsonSetter
    public void setLocation(String value) { supplied.add("location"); location = value; }

    @JsonSetter
    public void setStartsAt(Instant value) { supplied.add("startsAt"); startsAt = value; }

    @JsonSetter
    public void setEndsAt(Instant value) { supplied.add("endsAt"); endsAt = value; }

    @JsonSetter
    public void setCapacity(Integer value) { supplied.add("capacity"); capacity = value; }

    @JsonSetter
    public void setRegistrationDeadline(Instant value) {
        supplied.add("registrationDeadline");
        registrationDeadline = value;
    }

    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("수정할 수 없는 필드입니다: " + name);
    }

    @AssertTrue(message = "수정할 필드를 하나 이상 입력하고 제목·장소·시작·종료 시각에 유효한 값을 입력해 주세요.")
    @JsonIgnore
    public boolean isValidPatch() {
        return !supplied.isEmpty()
                && (!supplied.contains("title") || (title != null && !title.isBlank()))
                && (!supplied.contains("location") || (location != null && !location.isBlank()))
                && (!supplied.contains("startsAt") || startsAt != null)
                && (!supplied.contains("endsAt") || endsAt != null);
    }

    public UpdateEventCommand toCommand() {
        return new UpdateEventCommand(supplied, title, description, location,
                startsAt, endsAt, capacity, registrationDeadline);
    }
}
