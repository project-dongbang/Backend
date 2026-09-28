package com.dongbang.finance.application;

import com.dongbang.finance.domain.*;
import com.dongbang.finance.domain.repository.FeeItemRepository;
import com.dongbang.finance.domain.repository.FeeTargetRepository;
import com.dongbang.finance.exception.FinanceErrorCode;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.organization.application.facade.*;
import com.dongbang.organization.domain.MembershipRole;
import com.dongbang.organization.domain.MembershipStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeeDetailQueryServiceTest {
    @Mock FeeItemRepository items;
    @Mock FeeTargetRepository targets;
    @Mock MembershipAccessFacade memberships;
    @Mock OrganizationAccessFacade organizations;
    @InjectMocks FeeDetailQueryService service;

    @Test
    void staffSumsDifferentAssignedAmountsAndOnlyCurrentPaidTargets() {
        prepare(MembershipRole.ADMIN);
        var paid = target(9L, "40000.50");
        paid.changeStatus(FeeTargetStatus.PAID, null, 9L, Instant.now());
        var unpaid = target(10L, "20000.25");
        var reverted = target(11L, "10000.25");
        reverted.changeStatus(FeeTargetStatus.PAID, null, 9L, Instant.now());
        reverted.changeStatus(FeeTargetStatus.UNPAID, null, 9L, Instant.now());
        when(targets.findAllByFeeItemId(201L)).thenReturn(List.of(paid, unpaid, reverted));

        var result = service.detail(1L, 7L, 201L);

        assertThat(result.viewerType()).isEqualTo(FeeDetailResult.FeeViewerType.STAFF);
        assertThat(result.myPayment()).isNull();
        assertThat(result.staffSummary().targetCount()).isEqualTo(3);
        assertThat(result.staffSummary().paidCount()).isEqualTo(1);
        assertThat(result.staffSummary().unpaidCount()).isEqualTo(2);
        assertThat(result.staffSummary().collectedAmount()).isEqualByComparingTo("40000.50");
        assertThat(result.staffSummary().expectedAmount()).isEqualByComparingTo("70001.00");
        verify(targets, never()).findByFeeItemIdAndMembershipId(anyLong(), anyLong());
    }

    @Test
    void ownerWithNoTargetsGetsZeroSummary() {
        prepare(MembershipRole.OWNER);
        when(targets.findAllByFeeItemId(201L)).thenReturn(List.of());
        var result = service.detail(1L, 7L, 201L);
        assertThat(result.viewerType()).isEqualTo(FeeDetailResult.FeeViewerType.STAFF);
        assertThat(result.staffSummary().targetCount()).isZero();
        assertThat(result.staffSummary().expectedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.staffSummary().collectedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void memberReceivesOnlyOwnPaymentAndPaidTimestamp() {
        prepare(MembershipRole.MEMBER);
        var target = target(9L, "20000");
        Instant paidAt = Instant.parse("2026-09-09T09:00:00Z");
        target.changeStatus(FeeTargetStatus.PAID, null, 1L, paidAt);
        when(targets.findByFeeItemIdAndMembershipId(201L, 9L)).thenReturn(Optional.of(target));
        var result = service.detail(1L, 7L, 201L);
        assertThat(result.staffSummary()).isNull();
        assertThat(result.myPayment().amountDue()).isEqualByComparingTo("20000");
        assertThat(result.myPayment().status()).isEqualTo(FeeTargetStatus.PAID);
        assertThat(result.myPayment().paidAt()).isEqualTo(paidAt);
        verify(targets, never()).findAllByFeeItemId(anyLong());
    }

    @Test
    void nonTargetMemberReceivesSuccessfulNullPayment() {
        prepare(MembershipRole.MEMBER);
        when(targets.findByFeeItemIdAndMembershipId(201L, 9L)).thenReturn(Optional.empty());
        var result = service.detail(1L, 7L, 201L);
        assertThat(result.myPayment()).isNull();
        assertThat(result.staffSummary()).isNull();
    }

    @Test
    void missingMembershipCannotReadFeeItem() {
        when(memberships.getMembershipSummary(1L, 7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.detail(1L, 7L, 201L)).isInstanceOfSatisfying(GeneralException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(GeneralErrorCode.FORBIDDEN));
        verifyNoInteractions(items, targets);
    }

    @Test
    void inactiveMemberCannotReadFeeItem() {
        when(memberships.getMembershipSummary(1L, 7L)).thenReturn(Optional.of(
                new MembershipSummary(9L, 1L, 7L, "회원", MembershipRole.OWNER, MembershipStatus.LEFT)));
        assertThatThrownBy(() -> service.detail(1L, 7L, 201L)).isInstanceOfSatisfying(GeneralException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(GeneralErrorCode.FORBIDDEN));
        verifyNoInteractions(items, targets);
    }

    @Test
    void itemFromAnotherOrganizationIsNotFound() {
        member(MembershipRole.MEMBER);
        when(items.findByIdAndOrganizationId(201L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.detail(1L, 7L, 201L)).isInstanceOfSatisfying(GeneralException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(FinanceErrorCode.FEE_ITEM_NOT_FOUND));
        verifyNoInteractions(targets);
    }

    private void prepare(MembershipRole role) {
        member(role);
        var item = new FeeItem(1L, "정기 납부", LocalDate.of(2026, 9, 10), "설명", "은행", "123", "동방", 9L);
        ReflectionTestUtils.setField(item, "id", 201L);
        when(items.findByIdAndOrganizationId(201L, 1L)).thenReturn(Optional.of(item));
    }

    private void member(MembershipRole role) {
        when(memberships.getMembershipSummary(1L, 7L)).thenReturn(Optional.of(
                new MembershipSummary(9L, 1L, 7L, "회원", role, MembershipStatus.ACTIVE)));
    }

    private FeeTarget target(Long membershipId, String amount) {
        return new FeeTarget(201L, 1L, membershipId, new BigDecimal(amount));
    }
}
