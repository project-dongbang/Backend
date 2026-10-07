package com.dongbang.finance.application;

import com.dongbang.finance.domain.FeeTargetStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.dongbang.finance.presentation.dto.FinanceDtos.PaymentAccount;

public final class FeeDetailResult {
    private FeeDetailResult() {}

    public enum FeeViewerType { STAFF, MEMBER }
    public record FeeItemDetail(Long feeItemId, FeeViewerType viewerType, String title, LocalDate dueDate,
                                String description, FeeStaffSummary staffSummary, MyFeePayment myPayment,
                                FeeEditDetails editDetails) {}
    public record FeeEditDetails(PaymentAccount paymentAccount, boolean categoriesEditable,
                                 List<FeeEditCategory> categories) {}
    public record FeeEditCategory(Long categoryId, String name, BigDecimal amount,
                                  List<FeeEditTarget> targets) {}
    public record FeeEditTarget(Long feeTargetId, Long membershipId, BigDecimal amountDue,
                                FeeTargetStatus status) {}
    public record FeeStaffSummary(long targetCount, long paidCount, long unpaidCount,
                                  BigDecimal collectedAmount, BigDecimal expectedAmount) {}
    public record MyFeePayment(Long feeTargetId, BigDecimal amountDue, FeeTargetStatus status, Instant paidAt) {}
}
