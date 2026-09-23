package com.dongbang.event.presentation;

import com.dongbang.event.application.EventCommandService;
import com.dongbang.event.application.EventQueryService;
import com.dongbang.event.presentation.dto.request.CreateEventRequest;
import com.dongbang.event.presentation.dto.request.UpdateEventRequest;
import com.dongbang.event.presentation.dto.response.CalendarResponse;
import com.dongbang.event.presentation.dto.response.CreateEventResponse;
import com.dongbang.event.presentation.dto.response.EventDetailResponse;
import com.dongbang.global.response.ApiResponse;
import com.dongbang.global.response.code.GeneralSuccessCode;
import com.dongbang.global.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}")
@Tag(name = "일정·행사", description = "동아리 일정·행사 관리 API")
public class EventController {

    private final EventCommandService commandService;
    private final EventQueryService queryService;

    // 1. 월별 캘린더 조회
    @GetMapping("/calendar")
    @Operation(summary = "월별 캘린더 조회", description = "해당 월과 겹치는 동아리 일정·행사를 조회합니다.")
    public ApiResponse<CalendarResponse> calendar(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId,
            @RequestParam @Min(1) @Max(9999) int year,
            @RequestParam @Min(1) @Max(12) int month) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK,
                CalendarResponse.from(queryService.calendar(organizationId, userId, year, month)));
    }

    // 2. 일정·행사 등록
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "일정·행사 등록", description = "운영진이 일반 일정 또는 행사를 등록합니다.")
    public ApiResponse<CreateEventResponse> create(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId,
            @Valid @RequestBody CreateEventRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED,
                new CreateEventResponse(commandService.create(organizationId, userId, request.toCommand())));
    }

    // 3. 일정·행사 상세 조회
    @GetMapping("/events/{eventId}")
    @Operation(summary = "일정·행사 상세 조회", description = "동아리 일정·행사의 상세 정보를 조회합니다.")
    public ApiResponse<EventDetailResponse> detail(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long eventId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK,
                EventDetailResponse.from(queryService.detail(organizationId, userId, eventId)));
    }

    // 4. 일정·행사 수정
    @PatchMapping("/events/{eventId}")
    @Operation(summary = "일정·행사 수정", description = "운영진이 일정·행사 정보를 수정합니다.")
    public ApiResponse<Void> update(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long eventId,
            @Valid @RequestBody UpdateEventRequest request) {
        commandService.update(organizationId, userId, eventId, request.toCommand());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    // 5. 일정·행사 삭제
    @DeleteMapping("/events/{eventId}")
    @Operation(summary = "일정·행사 삭제", description = "운영진이 일정·행사를 삭제합니다.")
    public ApiResponse<Void> delete(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long eventId) {
        commandService.delete(organizationId, userId, eventId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }
}
