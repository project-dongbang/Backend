package com.dongbang.finance.presentation;

import com.dongbang.finance.application.FeeService;
import com.dongbang.finance.domain.FeeTargetStatus;
import com.dongbang.finance.presentation.dto.FinanceDtos.*;
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
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}")
@Tag(name = "납부 항목", description = "납부 항목과 회원별 납부 현황 API")
public class FeeController {
    private final FeeService feeService;

    @GetMapping("/fee-items")
    @Operation(summary = "납부 항목 목록 조회")
    public ApiResponse<PageResult<FeeItemListItem>> getFeeItems(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        if (!"createdAt,desc".equals(sort)) throw new com.dongbang.global.exception.GeneralException(com.dongbang.global.response.code.GeneralErrorCode.VALIDATION_ERROR);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, feeService.getFeeItems(organizationId, userId, page, size));
    }

    @PostMapping("/fee-items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "납부 항목 등록")
    public ApiResponse<FeeItemCreatedResult> createFeeItem(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @Valid @RequestBody CreateFeeItemRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, feeService.createFeeItem(organizationId, userId, request));
    }

    @PatchMapping("/fee-items/{feeItemId}")
    @Operation(summary = "납부 항목 수정")
    public ApiResponse<FeeItemUpdatedResult> updateFeeItem(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long feeItemId, @Valid @RequestBody UpdateFeeItemRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, feeService.updateFeeItem(organizationId, userId, feeItemId, request));
    }

    @DeleteMapping("/fee-items/{feeItemId}")
    @Operation(summary = "납부 항목 삭제")
    public ApiResponse<FeeItemDeletedResult> deleteFeeItem(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long feeItemId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, feeService.deleteFeeItem(organizationId, userId, feeItemId));
    }

    @GetMapping("/fee-targets")
    @Operation(summary = "납부 항목별 멤버 납부 현황 조회")
    public ApiResponse<CursorResult<FeeTargetItem>> getTargets(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @RequestParam @Positive Long feeItemId, @RequestParam(required = false) FeeTargetStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, feeService.getTargets(organizationId, userId, feeItemId, status, cursor, size));
    }

    @GetMapping("/fee-targets/me")
    @Operation(summary = "내 납부 내역 조회")
    public ApiResponse<PageResult<MyFeeTargetItem>> getMyTargets(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @RequestParam(required = false) FeeTargetStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "dueDate,desc") String sort) {
        if (!"dueDate,desc".equals(sort)) throw new com.dongbang.global.exception.GeneralException(com.dongbang.global.response.code.GeneralErrorCode.VALIDATION_ERROR);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, feeService.getMyTargets(organizationId, userId, status, page, size));
    }

    @PatchMapping("/fee-targets/{feeTargetId}/status")
    @Operation(summary = "멤버 납부 상태 변경")
    public ApiResponse<FeeTargetStatusResult> changeStatus(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long feeTargetId, @Valid @RequestBody ChangeFeeTargetStatusRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, feeService.changeStatus(organizationId, userId, feeTargetId, request));
    }

    @GetMapping("/fee-targets/export")
    @Operation(summary = "납부 현황 내보내기")
    public ResponseEntity<byte[]> exportTargets(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @RequestParam @Positive Long feeItemId, @RequestParam(required = false) FeeTargetStatus status,
            @RequestParam(defaultValue = "xlsx") String format) {
        validateFormat(format);
        byte[] file = feeService.exportTargets(organizationId, userId, feeItemId, status, format);
        return file(file, "fee-targets-" + feeItemId, format);
    }

    private void validateFormat(String format) {
        if (!"csv".equalsIgnoreCase(format) && !"xlsx".equalsIgnoreCase(format))
            throw new com.dongbang.global.exception.GeneralException(com.dongbang.global.response.code.GeneralErrorCode.VALIDATION_ERROR);
    }
    private ResponseEntity<byte[]> file(byte[] body, String name, String format) {
        MediaType type = "csv".equalsIgnoreCase(format) ? MediaType.parseMediaType("text/csv;charset=UTF-8")
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "." + format.toLowerCase() + "\"")
                .body(body);
    }
}
