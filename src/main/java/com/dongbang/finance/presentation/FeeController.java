package com.dongbang.finance.presentation;

import com.dongbang.finance.application.FeeService;
import com.dongbang.finance.application.FeeDetailQueryService;
import com.dongbang.finance.application.FeeDetailResult.FeeItemDetail;
import com.dongbang.finance.exception.FinanceErrorCode;
import com.dongbang.global.response.ApiErrorExamples;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.organization.exception.OrganizationErrorCode;
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
    private final FeeDetailQueryService feeDetailQueryService;

    @GetMapping("/fee-items/{feeItemId}")
    @Operation(summary = "회비 납부 마감 상세 조회", description = "운영진은 모금 현황을, 회원은 본인 납부 정보를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
            useReturnTypeSchema = true, content = @io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json", examples = {
                @io.swagger.v3.oas.annotations.media.ExampleObject(name = "운영진", value = """
                        {"isSuccess":true,"code":"COMMON_200_001","message":"성공적으로 요청을 처리했습니다.",
                         "result":{"feeItemId":201,"viewerType":"STAFF","title":"2026년 2학기 정기 납부",
                         "dueDate":"2026-09-10","description":"2학기 동아리 운영을 위한 정기 납부 항목입니다.",
                         "staffSummary":{"targetCount":64,"paidCount":58,"unpaidCount":6,
                         "collectedAmount":2320000,"expectedAmount":2560000},"myPayment":null},"errorDetail":null}
                        """),
                @io.swagger.v3.oas.annotations.media.ExampleObject(name = "일반 회원", value = """
                        {"isSuccess":true,"code":"COMMON_200_001","message":"성공적으로 요청을 처리했습니다.",
                         "result":{"feeItemId":201,"viewerType":"MEMBER","title":"2026년 2학기 정기 납부",
                         "dueDate":"2026-09-10","description":"2학기 동아리 운영을 위한 정기 납부 항목입니다.",
                         "staffSummary":null,"myPayment":{"feeTargetId":91,"amountDue":40000,
                         "status":"UNPAID","paidAt":null}},"errorDetail":null}
                        """),
                @io.swagger.v3.oas.annotations.media.ExampleObject(name = "납부 대상 아님", value = """
                        {"isSuccess":true,"code":"COMMON_200_001","message":"성공적으로 요청을 처리했습니다.",
                         "result":{"feeItemId":201,"viewerType":"MEMBER","title":"2026년 2학기 정기 납부",
                         "dueDate":"2026-09-10","description":null,"staffSummary":null,"myPayment":null},
                         "errorDetail":null}
                        """)
            }))
    @ApiErrorExamples(value = GeneralErrorCode.class, names = {"VALIDATION_ERROR", "UNAUTHORIZED", "FORBIDDEN"})
    @ApiErrorExamples(value = OrganizationErrorCode.class, names = {"ORGANIZATION_NOT_FOUND"})
    @ApiErrorExamples(value = FinanceErrorCode.class, names = {"FEE_ITEM_NOT_FOUND"})
    public ApiResponse<FeeItemDetail> detail(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @PathVariable @Positive Long organizationId, @PathVariable @Positive Long feeItemId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK,
                feeDetailQueryService.detail(organizationId, userId, feeItemId));
    }

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
