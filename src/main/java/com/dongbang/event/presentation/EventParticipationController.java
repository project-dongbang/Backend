package com.dongbang.event.presentation;

import com.dongbang.event.application.EventParticipationService;
import com.dongbang.event.application.EventParticipationService.ParticipantList;
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
@Tag(name = "일정·행사")
public class EventParticipationController {
    private final EventParticipationService service;

    @PostMapping("/participants/me")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "행사 참가 신청")
    public ApiResponse<Void> apply(@Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId, @PathVariable @Positive Long eventId) {
        service.apply(organizationId, userId, eventId);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, null);
    }

    @DeleteMapping("/participants/me")
    @Operation(summary = "본인 참가 신청 취소", description = "신청 마감 전 취소할 수 있습니다.")
    public ApiResponse<Void> withdraw(@Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId, @PathVariable @Positive Long eventId) {
        service.withdraw(organizationId, userId, eventId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    @GetMapping("/participants")
    @Operation(summary = "참가자 목록 조회", description = "운영진 전용. 참가자 목록과 변경 검증용 버전을 반환합니다.")
    public ApiResponse<ParticipantList> list(@Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId, @PathVariable @Positive Long eventId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, service.list(organizationId, userId, eventId));
    }

    @PostMapping("/participants")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "운영진 참가자 추가", description = "행사 시작 전 정원 이내에서 활동 회원을 추가합니다.")
    public ApiResponse<Void> add(@Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId, @PathVariable @Positive Long eventId,
            @Valid @RequestBody AddParticipantRequest request) {
        service.addParticipant(organizationId, userId, eventId, request.membershipId(), request.participantVersion());
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, null);
    }

    @DeleteMapping("/participants/{membershipId}")
    @Operation(summary = "운영진 참가자 삭제")
    public ApiResponse<Void> remove(@Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId, @PathVariable @Positive Long eventId,
            @PathVariable @Positive Long membershipId, @RequestParam @PositiveOrZero long participantVersion) {
        service.removeParticipant(organizationId, userId, eventId, membershipId, participantVersion);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    @PostMapping("/registration/close")
    @Operation(summary = "행사 신청 조기 마감", description = "운영진 전용. 이미 마감했으면 성공을 반환합니다.")
    public ApiResponse<Void> close(@Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId, @PathVariable @Positive Long eventId) {
        service.closeRegistration(organizationId, userId, eventId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    @PostMapping("/cancel")
    @Operation(summary = "일정·행사 취소", description = "운영진 전용. 캘린더에는 유지하고 행사 신청을 차단합니다.")
    public ApiResponse<Void> cancel(@Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId, @PathVariable @Positive Long eventId) {
        service.cancel(organizationId, userId, eventId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    public record AddParticipantRequest(@NotNull @Positive Long membershipId,
                                        @NotNull @PositiveOrZero Long participantVersion) {}
}
