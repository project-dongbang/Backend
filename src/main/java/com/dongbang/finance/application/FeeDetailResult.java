package com.dongbang.finance.application;

import com.dongbang.finance.domain.FeeTargetStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class FeeDetailResult {
    private FeeDetailResult() {}

    public enum FeeViewerType { STAFF, MEMBER }
    public record FeeItemDetail(Long feeItemId, FeeViewerType viewerType, String title, LocalDate dueDate,
                                String description, FeeStaffSummary staffSummary, MyFeePayment myPayment) {}
    public record FeeStaffSummary(long targetCount, long paidCount, long unpaidCount,
                                  BigDecimal collectedAmount, BigDecimal expectedAmount) {}
    public record MyFeePayment(Long feeTargetId, BigDecimal amountDue, FeeTargetStatus status, Instant paidAt) {}
}
