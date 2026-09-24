package com.dongbang.attendance.presentation;

import com.dongbang.attendance.application.AttendanceService;
import com.dongbang.attendance.application.dto.AttendanceResults.*;
import com.dongbang.attendance.presentation.dto.request.CheckInRequest;
import com.dongbang.attendance.presentation.dto.request.ModifyAttendanceRequest;
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
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}")
@Tag(name = "QR 출석", description = "동아리 QR 출석 관리 API")
public class AttendanceController {
    private final AttendanceService service;

    @GetMapping("/attendance/events")
    @Operation(summary = "출석 행사 목록 조회", description = "운영진이 출석 관리 대상 행사를 조회합니다.")
    public ApiResponse<EventList> events(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "START_DESC") EventSort sort,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "4") @Min(1) @Max(100) int size) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK,
                service.events(organizationId, userId, keyword, startDate, endDate, sort, page, size));
    }

    @GetMapping("/events/{eventId}/attendance")
    @Operation(summary = "출석 현황 조회", description = "운영진이 행사 출석 현황과 기존 QR을 조회합니다.")
    public ApiResponse<Status> status(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long eventId,
            @RequestParam(defaultValue = "ALL") RecordFilter status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK,
                service.status(organizationId, userId, eventId, status, keyword, page, size));
    }

    @PostMapping("/events/{eventId}/attendance/session")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "출석 시작 및 QR 생성", description = "운영진이 10분 동안 유효한 QR을 한 번 생성합니다.")
    public ApiResponse<Started> start(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long eventId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, service.start(organizationId, userId, eventId));
    }

    @PostMapping("/events/{eventId}/attendance/session/close")
    @Operation(summary = "출석 종료", description = "운영진이 QR 출석을 조기 종료합니다.")
    public ApiResponse<Void> close(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long eventId) {
        service.close(organizationId, userId, eventId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    @PatchMapping("/events/{eventId}/attendance/{attendanceId}")
    @Operation(summary = "출석 상태 수정", description = "운영진이 회원 출석 상태를 직접 수정합니다.")
    public ApiResponse<Void> modify(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long eventId,
            @PathVariable @Positive Long attendanceId,
            @Valid @RequestBody ModifyAttendanceRequest request) {
        service.modify(organizationId, userId, eventId, attendanceId, request.toCommand());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    @PostMapping("/events/{eventId}/attendance/check-in")
    @Operation(summary = "QR 출석 체크인", description = "행사 참가자가 유효한 QR로 출석합니다.")
    public ApiResponse<CheckIn> checkIn(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long eventId,
            @Valid @RequestBody CheckInRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK,
                service.checkIn(organizationId, userId, eventId, request.qrToken()));
    }

    @GetMapping("/attendance/me")
    @Operation(summary = "내 출석 목록 조회", description = "회원이 본인의 행사별 출석 결과를 조회합니다.")
    public ApiResponse<MyList> mine(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, service.mine(organizationId, userId, page, size));
    }
}
