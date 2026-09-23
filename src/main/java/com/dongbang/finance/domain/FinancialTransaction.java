package com.dongbang.finance.domain;

import com.dongbang.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "financial_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinancialTransaction extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id") private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @Column(name = "fee_item_id") private Long feeItemId;
    @Column(name = "fee_target_id") private Long feeTargetId;
    @Enumerated(EnumType.STRING) @Column(name = "transaction_type", nullable = false, length = 20) private TransactionType transactionType;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, length = 50) private String category;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(name = "occurred_on", nullable = false) private LocalDate occurredOn;
    @Column(nullable = false, length = 255) private String vendor;
    @Column(name = "payment_method", length = 50) private String paymentMethod;
    @Column(length = 1000) private String memo;
    @Column(name = "evidence_file_id") private Long evidenceFileId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TransactionStatus status;
    @Column(name = "created_by_membership_id", nullable = false) private Long createdByMembershipId;
    @Column(name = "void_reason", length = 255) private String voidReason;
    @Column(name = "voided_by_membership_id") private Long voidedByMembershipId;
    @Column(name = "voided_at") private Instant voidedAt;

    public FinancialTransaction(Long organizationId, Long feeItemId, Long feeTargetId, TransactionType type,
                                String title, String category, BigDecimal amount, LocalDate occurredOn,
                                String vendor, String paymentMethod, String memo, Long evidenceFileId, Long creatorId) {
        this.organizationId = organizationId; this.feeItemId = feeItemId; this.feeTargetId = feeTargetId;
        this.transactionType = type; this.title = title; this.category = category; this.amount = amount;
        this.occurredOn = occurredOn; this.vendor = vendor; this.paymentMethod = paymentMethod;
        this.memo = memo; this.evidenceFileId = evidenceFileId; this.createdByMembershipId = creatorId;
        this.status = TransactionStatus.POSTED;
    }

    public void update(String title, BigDecimal amount, LocalDate occurredOn, String vendor,
                       String category, String paymentMethod, String memo, boolean memoPresent, Long evidenceFileId) {
        if (title != null) this.title = title;
        if (amount != null) this.amount = amount;
        if (occurredOn != null) this.occurredOn = occurredOn;
        if (vendor != null) this.vendor = vendor;
        if (category != null) this.category = category;
        if (paymentMethod != null) this.paymentMethod = paymentMethod;
        if (memoPresent) this.memo = memo;
        if (evidenceFileId != null) this.evidenceFileId = evidenceFileId;
    }

    public void voidTransaction(String reason, Long membershipId, Instant at) {
        if (status == TransactionStatus.VOID) return;
        this.status = TransactionStatus.VOID; this.voidReason = reason;
        this.voidedByMembershipId = membershipId; this.voidedAt = at;
    }

    public void detachFeeReferences() { this.feeItemId = null; this.feeTargetId = null; }
}
