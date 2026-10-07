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
    @Operation(summary = "회비 납부 마감 상세 조회", description = "운영진은 모금 현황과 편집용 계좌·카테고리·대상 목록을 조회합니다. 회원은 본인 납부 정보만 조회하며 editDetails는 null입니다. categoriesEditable이 false면 납부 이력이 있어 카테고리 교체가 불가능합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
            useReturnTypeSchema = true, content = @io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json", examples = {
                @io.swagger.v3.oas.annotations.media.ExampleObject(name = "운영진", value = """
                        {"isSuccess":true,"code":"COMMON_200_001","message":"성공적으로 요청을 처리했습니다.",
                         "result":{"feeItemId":201,"viewerType":"STAFF","title":"2026년 2학기 정기 납부",
                         "dueDate":"2026-09-10","description":"2학기 동아리 운영을 위한 정기 납부 항목입니다.",
                         "staffSummary":{"targetCount":64,"paidCount":58,"unpaidCount":6,
                         "collectedAmount":2320000,"expectedAmount":2560000},"myPayment":null,
                         "editDetails":{"paymentAccount":{"bankName":"국민은행","accountNumber":"123-456","accountHolder":"동방"},
                         "categoriesEditable":false,"categories":[{"categoryId":31,"name":"정기 회비","amount":40000,
                         "targets":[{"feeTargetId":91,"membershipId":12,"amountDue":40000,"status":"PAID"}]}]}},"errorDetail":null}
                        """),
                @io.swagger.v3.oas.annotations.media.ExampleObject(name = "일반 회원", value = """
                        {"isSuccess":true,"code":"COMMON_200_001","message":"성공적으로 요청을 처리했습니다.",
                         "result":{"feeItemId":201,"viewerType":"MEMBER","title":"2026년 2학기 정기 납부",
                         "dueDate":"2026-09-10","description":"2학기 동아리 운영을 위한 정기 납부 항목입니다.",
                         "staffSummary":null,"myPayment":{"feeTargetId":91,"amountDue":40000,
                         "status":"UNPAID","paidAt":null},"editDetails":null},"errorDetail":null}
                        """),
                @io.swagger.v3.oas.annotations.media.ExampleObject(name = "납부 대상 아님", value = """
                        {"isSuccess":true,"code":"COMMON_200_001","message":"성공적으로 요청을 처리했습니다.",
                         "result":{"feeItemId":201,"viewerType":"MEMBER","title":"2026년 2학기 정기 납부",
                         "dueDate":"2026-09-10","description":null,"staffSummary":null,"myPayment":null,"editDetails":null},
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
    @Operation(summary = "납부 항목 수정", description = "운영진 전용. 제목·기한·설명·입금 계좌는 부분 수정합니다. categories를 보내면 목록 전체를 교체하고 카테고리/대상 ID가 새로 발급됩니다. 각 카테고리의 amount가 대상별 부과 금액으로 적용됩니다. categories는 비어 있거나 null일 수 없고, 이미 납부 이력(무효화된 수입 포함)이 있으면 409 FEE_409_004를 반환합니다. categories를 생략하면 기존 카테고리·대상은 유지합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json", examples = {
                @io.swagger.v3.oas.annotations.media.ExampleObject(name = "기본 정보만 수정", value = """
                        {"title":"2026년 2학기 회비","dueDate":"2026-09-30",
                         "paymentAccount":{"bankName":"국민은행","accountNumber":"123-456","accountHolder":"동방"}}
                        """),
                @io.swagger.v3.oas.annotations.media.ExampleObject(name = "카테고리·대상 전체 교체", value = """
                        {"categories":[{"name":"정기 회비","amount":40000,"targetMembershipIds":[12,13]},
                                       {"name":"신입 회비","amount":20000,"targetMembershipIds":[14]}]}
                        """)
            }))
    @ApiErrorExamples(value = FinanceErrorCode.class, names = {"FEE_ITEM_NOT_FOUND", "INVALID_TARGET", "DUPLICATE_TARGET", "CATEGORY_EDIT_AFTER_PAYMENT"})
    @ApiErrorExamples(value = GeneralErrorCode.class, names = {"VALIDATION_ERROR", "UNAUTHORIZED", "FORBIDDEN"})
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
