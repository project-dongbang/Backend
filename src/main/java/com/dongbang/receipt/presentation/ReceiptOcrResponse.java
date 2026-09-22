package com.dongbang.receipt.presentation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReceiptOcrResponse(
        String text,
        String itemTitle,
        LocalDate spentOn,
        BigDecimal amount,
        String merchant,
        String category,
        String paymentMethod,
        String memo,
        String storeName,
        LocalDate purchasedAt,
        BigDecimal totalAmount,
        List<ReceiptItem> items,
        List<OcrBlock> blocks
) {
    public record OcrBlock(String text, double confidence) {
    }

    public record ReceiptItem(String name, Integer quantity, BigDecimal price) {
    }
}
