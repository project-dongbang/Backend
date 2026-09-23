package com.dongbang.finance.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceDomainTest {
    @Test
    void changingFeeTargetStatusMaintainsPaidAtInvariant() {
        FeeTarget target = new FeeTarget(1L, 2L, 3L, BigDecimal.valueOf(40000));
        Instant changedAt = Instant.parse("2026-09-23T00:00:00Z");

        target.changeStatus(FeeTargetStatus.PAID, "입금 확인", 7L, changedAt);
        assertThat(target.getPaidAt()).isEqualTo(changedAt);

        target.changeStatus(FeeTargetStatus.UNPAID, null, 7L, changedAt.plusSeconds(60));
        assertThat(target.getPaidAt()).isNull();
    }

    @Test
    void voidingTransactionIsIdempotent() {
        FinancialTransaction transaction = new FinancialTransaction(1L, null, null, TransactionType.EXPENSE,
                "간식", "행사 운영", BigDecimal.valueOf(10000), LocalDate.of(2026, 9, 23),
                "마트", "카드", null, 5L, 7L);
        Instant first = Instant.parse("2026-09-23T00:00:00Z");
        transaction.voidTransaction("운영진 삭제", 7L, first);
        transaction.voidTransaction("다른 사유", 8L, first.plusSeconds(10));

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.VOID);
        assertThat(transaction.getVoidReason()).isEqualTo("운영진 삭제");
        assertThat(transaction.getVoidedByMembershipId()).isEqualTo(7L);
        assertThat(transaction.getVoidedAt()).isEqualTo(first);
    }
}
