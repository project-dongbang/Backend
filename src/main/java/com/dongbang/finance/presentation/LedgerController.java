package com.dongbang.finance.presentation;

import com.dongbang.finance.application.LedgerService;
import com.dongbang.finance.domain.TransactionType;
import com.dongbang.finance.presentation.dto.FinanceDtos.*;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.ApiResponse;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.global.response.code.GeneralSuccessCode;
import com.dongbang.global.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}/ledger")
@Tag(name = "공개 장부", description = "입출금 내역과 증빙 API")
public class LedgerController {
    private final LedgerService ledgerService;

    @GetMapping
    @Operation(summary = "공개 장부 목록·잔액 조회")
    public ApiResponse<CursorResult<LedgerItem>> getLedger(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) String category, @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK,
                ledgerService.getLedger(organizationId, userId, from, to, transactionType, category, cursor, size));
    }

    @PostMapping(value = "/expenses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "지출 내역·영수증 등록")
    public ApiResponse<TransactionResult> createExpense(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @Valid @RequestPart("request") ExpenseRequest request,
            @RequestPart("evidence") MultipartFile evidence) {
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, ledgerService.createExpense(organizationId, userId, request, evidence));
    }

    @PostMapping("/incomes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "수입 내역 등록")
    public ApiResponse<TransactionResult> createIncome(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @Valid @RequestBody IncomeRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, ledgerService.createIncome(organizationId, userId, request));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "공개 장부 항목 상세·증빙 조회")
    public ApiResponse<TransactionDetail> getDetail(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long transactionId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, ledgerService.getDetail(organizationId, userId, transactionId));
    }

    @PatchMapping("/incomes/{transactionId}")
    @Operation(summary = "수입 내역 수정")
    public ApiResponse<TransactionResult> updateIncome(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long transactionId, @Valid @RequestBody UpdateIncomeRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, ledgerService.updateIncome(organizationId, userId, transactionId, request));
    }

    @PatchMapping(value = "/expenses/{transactionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "지출 내역·영수증 수정")
    public ApiResponse<TransactionResult> updateExpense(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long transactionId,
            @Valid @RequestPart(value = "request", required = false) UpdateExpenseRequest request,
            @RequestPart(value = "evidence", required = false) MultipartFile evidence) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK,
                ledgerService.updateExpense(organizationId, userId, transactionId, request, evidence));
    }

    @DeleteMapping("/expenses/{transactionId}")
    @Operation(summary = "지출 내역 삭제")
    public ApiResponse<DeletedExpenseResult> deleteExpense(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long transactionId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, ledgerService.deleteExpense(organizationId, userId, transactionId));
    }

    @GetMapping("/export")
    @Operation(summary = "공개 장부 내보내기")
    public ResponseEntity<byte[]> exportLedger(
            @Parameter(hidden = true) @CurrentUserId Long userId, @PathVariable @Positive Long organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) String category, @RequestParam(defaultValue = "xlsx") String format) {
        if (!"csv".equalsIgnoreCase(format) && !"xlsx".equalsIgnoreCase(format)) throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        byte[] body = ledgerService.exportLedger(organizationId, userId, from, to, transactionType, category, format);
        MediaType type = "csv".equalsIgnoreCase(format) ? MediaType.parseMediaType("text/csv;charset=UTF-8")
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ledger-" + organizationId + "." + format.toLowerCase() + "\"")
                .body(body);
    }
}
