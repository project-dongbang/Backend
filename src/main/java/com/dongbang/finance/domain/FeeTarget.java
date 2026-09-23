package com.dongbang.finance.domain;

import com.dongbang.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fee_targets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeeTarget extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_target_id") private Long id;
    @Column(name = "fee_item_id", nullable = false) private Long feeItemId;
    @Column(name = "fee_category_id", nullable = false) private Long feeCategoryId;
    @Column(name = "membership_id", nullable = false) private Long membershipId;
    @Column(name = "amount_due", nullable = false, precision = 14, scale = 2) private BigDecimal amountDue;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private FeeTargetStatus status;
    @Column(name = "status_memo", length = 200) private String statusMemo;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "status_changed_at") private Instant statusChangedAt;
    @Column(name = "status_changed_by_membership_id") private Long statusChangedByMembershipId;

    public FeeTarget(Long feeItemId, Long categoryId, Long membershipId, BigDecimal amountDue) {
        this.feeItemId = feeItemId; this.feeCategoryId = categoryId; this.membershipId = membershipId;
        this.amountDue = amountDue; this.status = FeeTargetStatus.UNPAID;
    }

    public void changeStatus(FeeTargetStatus status, String memo, Long changerId, Instant changedAt) {
        this.status = status; this.statusMemo = memo; this.statusChangedByMembershipId = changerId;
        this.statusChangedAt = changedAt; this.paidAt = status == FeeTargetStatus.PAID ? changedAt : null;
    }
}
