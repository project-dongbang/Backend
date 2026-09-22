package com.dongbang.receipt.presentation;

import com.dongbang.global.response.ApiResponse;
import com.dongbang.global.response.code.GeneralSuccessCode;
import com.dongbang.global.security.CurrentUserId;
import com.dongbang.receipt.application.ReceiptCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Tag(name = "영수증", description = "영수증 OCR 인식 API")
public class ReceiptController {
    private final ReceiptCommandService receiptCommandService;

    @PostMapping(value = "/api/v1/organizations/{organizationId}/receipts/ocr",
            consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "영수증 OCR 인식", description = "영수증 이미지에서 텍스트와 인식 신뢰도를 추출합니다.")
    public ApiResponse<ReceiptResponse> recognize(
            @CurrentUserId Long userId,
            @PathVariable Long organizationId,
            @RequestBody(content = @Content(mediaType = "multipart/form-data",
                    schema = @Schema(implementation = ReceiptOcrRequest.class)))
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, receiptCommandService.create(organizationId, userId, file));
    }
}
