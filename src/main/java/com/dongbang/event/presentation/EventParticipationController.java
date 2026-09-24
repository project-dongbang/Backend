package com.dongbang.event.presentation;

import com.dongbang.event.application.EventParticipationService;
import com.dongbang.event.application.ParticipantCandidateService;
import com.dongbang.event.application.result.ParticipantCandidatesResult;
import com.dongbang.event.application.result.ChangeParticipantsResult;
import com.dongbang.event.application.result.EventApplicationResult;
import com.dongbang.event.presentation.dto.request.ChangeParticipantsRequest;
import com.dongbang.global.response.ApiResponse;
import com.dongbang.global.response.code.GeneralSuccessCode;
import com.dongbang.global.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}/events/{eventId}")
@Tag(name = "일정·행사", description = "동아리 일정·행사 관리 API")
public class EventParticipationController {
    private final EventParticipationService service;
    private final ParticipantCandidateService candidateService;

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "행사 참가 신청")
    public ApiResponse<EventApplicationResult> apply(@Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId, @PathVariable @Positive Long eventId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, service.apply(organizationId, userId, eventId));
    }

    @DeleteMapping("/applications")
    @Operation(summary = "본인 참가 신청 취소", description = "신청 마감 전 취소할 수 있습니다.")
    public ApiResponse<Void> withdraw(@Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId, @PathVariable @Positive Long eventId) {
        service.withdraw(organizationId, userId, eventId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    @PostMapping("/applications/close")
    @Operation(summary = "행사 신청 조기 마감", description = "운영진 전용. 이미 마감했으면 성공을 반환합니다.")
    public ApiResponse<Void> close(@Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId, @PathVariable @Positive Long eventId) {
        service.closeRegistration(organizationId, userId, eventId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    @GetMapping("/participant-candidates")
    @Operation(summary = "행사 참가자 직접 수정 대상 조회")
    public ApiResponse<ParticipantCandidatesResult> candidates(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId, @PathVariable @Positive Long eventId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK,
                candidateService.candidates(organizationId, userId, eventId, keyword, page, size));
    }

    @PatchMapping("/participants")
    @Operation(summary = "행사 참가자 직접 수정 저장")
    public ApiResponse<ChangeParticipantsResult> changeParticipants(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId, @PathVariable @Positive Long eventId,
            @Valid @RequestBody ChangeParticipantsRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK,
                service.changeParticipants(organizationId, userId, eventId, request.toCommand()));
    }
}
