package com.dongbang.receipt.presentation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import java.util.List;

public record ReceiptResponse(
        Long receiptId,
        String originalName,
        String contentType,
        long sizeBytes,
        String storageKey,
        String fileUrl,
        String itemTitle,
        LocalDate spentOn,
        BigDecimal amount,
        String merchant,
        String category,
        String paymentMethod,
        String memo,
        List<ReceiptOcrResponse.ReceiptItem> items,
        String storeName,
        LocalDate purchasedAt,
        BigDecimal totalAmount,
        String ocrText,
        Instant createdAt
) {
}
