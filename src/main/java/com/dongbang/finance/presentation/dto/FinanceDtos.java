package com.dongbang.finance.presentation.dto;

import com.dongbang.finance.domain.FeeTargetStatus;
import com.dongbang.finance.domain.TransactionStatus;
import com.dongbang.finance.domain.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class FinanceDtos {
    private FinanceDtos() {}

    public record PaymentAccount(
            @NotBlank @Size(max = 50) String bankName,
            @NotBlank @Size(max = 50) @Pattern(regexp = "^[0-9-]+$") String accountNumber,
            @NotBlank @Size(max = 100) String accountHolder) {}

    public record CreateCategory(
            @NotBlank @Size(max = 100) String name,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotEmpty List<@Positive Long> targetMembershipIds) {}

    public record CreateFeeItemRequest(
            @NotBlank @Size(max = 200) String title,
            @NotNull LocalDate dueDate,
            @Size(max = 1000) String description,
            @NotNull @Valid PaymentAccount paymentAccount,
            @NotEmpty List<@Valid CreateCategory> categories) {}

    public static final class UpdateFeeItemRequest {
        @Size(min = 1, max = 200) private String title;
        private LocalDate dueDate;
        @Size(max = 1000) private String description;
        @Valid private PaymentAccount paymentAccount;
        private boolean descriptionPresent;
        public String title() { return title; }
        public LocalDate dueDate() { return dueDate; }
        public String description() { return description; }
        public PaymentAccount paymentAccount() { return paymentAccount; }
        public void setTitle(String title) { this.title = title; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        @JsonSetter("description") public void setDescription(String description) { this.description = description; this.descriptionPresent = true; }
        public void setPaymentAccount(PaymentAccount paymentAccount) { this.paymentAccount = paymentAccount; }
        public boolean descriptionPresent() { return descriptionPresent; }
        public boolean isEmpty() { return title == null && dueDate == null && !descriptionPresent && paymentAccount == null; }
    }

    public record CategoryResult(Long categoryId, String name, BigDecimal amount, long targetCount) {}
    public record FeeItemCreatedResult(Long feeItemId, String title, LocalDate dueDate, PaymentAccount paymentAccount,
                                       int categoryCount, int targetCount, BigDecimal expectedAmount,
                                       List<CategoryResult> categories, Instant createdAt) {}
    public record FeeItemListItem(Long feeItemId, String title, long categoryCount, long targetCount,
                                  BigDecimal expectedAmount, LocalDate dueDate) {}
    public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
    public record FeeItemUpdatedResult(Long feeItemId, String title, LocalDate dueDate, String description,
                                       PaymentAccount paymentAccount, long categoryCount,
                                       BigDecimal expectedAmount, Instant updatedAt) {}
    public record FeeItemDeletedResult(Long feeItemId, long deletedCategoryCount, long deletedTargetCount,
                                       long voidedIncomeCount, Long deletedByMembershipId, Instant deletedAt) {}

    public record FeeTargetSummary(Long feeItemId, long targetCount, long paidCount, long unpaidCount, double paymentRate) {}
    public record FeeTargetItem(Long feeTargetId, Long membershipId, String name, String studentNumber,
                                String generation, Long categoryId, String categoryName, BigDecimal amountDue,
                                FeeTargetStatus status, Instant paidAt) {}
    public record CursorResult<T>(Object summary, List<T> content, int size, String nextCursor, boolean hasNext) {}
    public record MyFeeTargetItem(Long feeTargetId, Long feeItemId, String title, Long categoryId,
                                  String categoryName, BigDecimal amountDue, LocalDate dueDate,
                                  PaymentAccount paymentAccount, String description, FeeTargetStatus status, Instant paidAt) {}

    public record ChangeFeeTargetStatusRequest(@NotNull FeeTargetStatus status, @Size(max = 200) String memo) {}
    public record FeeTargetStatusResult(Long feeTargetId, FeeTargetStatus status, String memo, Instant paidAt,
                                        Instant statusChangedAt, Long statusChangedByMembershipId,
                                        Long incomeTransactionId) {}

    public record ExpenseRequest(@NotBlank @Size(max = 200) String title, @NotNull LocalDate occurredOn,
                                 @NotNull @DecimalMin("0.01") BigDecimal amount,
                                 @NotBlank @Size(max = 255) String counterparty,
                                 @NotBlank @Size(max = 50) String category,
                                 @NotBlank @Size(max = 50) String paymentMethod,
                                 @Size(max = 1000) String memo) {}
    public record IncomeRequest(@NotBlank @Size(max = 200) String title,
                                @NotBlank @Size(max = 255) String counterparty,
                                @NotNull @DecimalMin("0.01") BigDecimal amount,
                                @NotNull LocalDate occurredOn,
                                @Size(max = 1000) String memo) {}
    public static final class UpdateIncomeRequest {
        @Size(min = 1, max = 200) private String title;
        @Size(min = 1, max = 255) private String counterparty;
        @DecimalMin("0.01") private BigDecimal amount;
        private LocalDate occurredOn;
        @Size(max = 1000) private String memo;
        private boolean memoPresent;
        public String title() { return title; }
        public String counterparty() { return counterparty; }
        public BigDecimal amount() { return amount; }
        public LocalDate occurredOn() { return occurredOn; }
        public String memo() { return memo; }
        public boolean memoPresent() { return memoPresent; }
        public void setTitle(String title) { this.title = title; }
        public void setCounterparty(String counterparty) { this.counterparty = counterparty; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public void setOccurredOn(LocalDate occurredOn) { this.occurredOn = occurredOn; }
        @JsonSetter("memo") public void setMemo(String memo) { this.memo = memo; this.memoPresent = true; }
        public boolean isEmpty() { return title == null && counterparty == null && amount == null && occurredOn == null && !memoPresent; }
    }
    public static final class UpdateExpenseRequest {
        @Size(min = 1, max = 200) private String title;
        private LocalDate occurredOn;
        @DecimalMin("0.01") private BigDecimal amount;
        @Size(min = 1, max = 255) private String counterparty;
        @Size(min = 1, max = 50) private String category;
        @Size(min = 1, max = 50) private String paymentMethod;
        @Size(max = 1000) private String memo;
        private boolean memoPresent;
        public String title() { return title; }
        public LocalDate occurredOn() { return occurredOn; }
        public BigDecimal amount() { return amount; }
        public String counterparty() { return counterparty; }
        public String category() { return category; }
        public String paymentMethod() { return paymentMethod; }
        public String memo() { return memo; }
        public boolean memoPresent() { return memoPresent; }
        public void setTitle(String title) { this.title = title; }
        public void setOccurredOn(LocalDate occurredOn) { this.occurredOn = occurredOn; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public void setCounterparty(String counterparty) { this.counterparty = counterparty; }
        public void setCategory(String category) { this.category = category; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        @JsonSetter("memo") public void setMemo(String memo) { this.memo = memo; this.memoPresent = true; }
        public boolean isEmpty() { return title == null && occurredOn == null && amount == null && counterparty == null
                && category == null && paymentMethod == null && !memoPresent; }
    }

    public record TransactionResult(Long transactionId, TransactionType transactionType, String title, String category,
                                    BigDecimal amount, LocalDate occurredOn, String counterparty, String paymentMethod,
                                    String memo, TransactionStatus status, Long evidenceFileId,
                                    BigDecimal balanceAfter, Instant createdAt, Instant updatedAt) {}
    public record LedgerPeriod(LocalDate from, LocalDate to, BigDecimal expenseAmount, long expenseCount) {}
    public record LedgerSummary(BigDecimal balance, BigDecimal totalIncome, BigDecimal totalExpense, LedgerPeriod period) {}
    public record LedgerItem(String entryType, Long transactionId, Long feeItemId, TransactionType transactionType,
                             String title, String category, BigDecimal amount, LocalDate occurredOn, String counterparty,
                             String createdByName, TransactionStatus status, boolean hasEvidence,
                             Long aggregatedTransactionCount) {}
    public record CreatedBy(Long membershipId, String name) {}
    public record Evidence(Long fileId, String originalName, String contentType, Long sizeBytes, String downloadUrl) {}
    public record TransactionDetail(Long transactionId, Long feeItemId, TransactionType transactionType,
                                    String title, String category, BigDecimal amount, LocalDate occurredOn,
                                    String counterparty, String paymentMethod, String memo, TransactionStatus status,
                                    String voidReason, Long voidedByMembershipId, Instant voidedAt,
                                    CreatedBy createdBy, Evidence evidence, Instant createdAt) {}
    public record DeletedExpenseResult(Long transactionId, TransactionStatus status,
                                       Long deletedByMembershipId, Instant deletedAt, BigDecimal balanceAfter) {}
}
