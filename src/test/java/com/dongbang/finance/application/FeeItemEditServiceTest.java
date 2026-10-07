package com.dongbang.finance.application;

import com.dongbang.finance.domain.*;
import com.dongbang.finance.domain.repository.*;
import com.dongbang.finance.exception.FinanceErrorCode;
import com.dongbang.finance.presentation.dto.FinanceDtos.CreateCategory;
import com.dongbang.finance.presentation.dto.FinanceDtos.UpdateFeeItemRequest;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.organization.domain.Organization;
import com.dongbang.organization.domain.repository.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeeItemEditServiceTest {
    @Mock FeeItemRepository items;
    @Mock FeeCategoryRepository categories;
    @Mock FeeTargetRepository targets;
    @Mock FinancialTransactionRepository transactions;
    @Mock AuditLogRepository audits;
    @Mock MembershipRepository members;
    @Mock FinanceAccessService access;
    @InjectMocks FeeService service;

    private FeeItem item;
    private Membership actor;

    @BeforeEach
    void setUp() {
        item = new FeeItem(1L, "기존 회비", LocalDate.of(2026, 9, 10), null, "은행", "123", "동방", 9L);
        ReflectionTestUtils.setField(item, "id", 201L);
    }

    @Test
    void categoryReplacementIsRejectedAfterPaymentHistory() {
        prepareActor();
        var request = replacement(List.of(new CreateCategory("새 회비", new BigDecimal("50000"), List.of(9L))));
        prepareValidMember(9L);
        when(transactions.existsByFeeItemId(201L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateFeeItem(1L, 7L, 201L, request))
                .isInstanceOfSatisfying(GeneralException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(FinanceErrorCode.CATEGORY_EDIT_AFTER_PAYMENT));
        verify(categories, never()).deleteAll(any());
        verify(targets, never()).deleteAll(any());
        assertThat(item.getTitle()).isEqualTo("기존 회비");
    }

    @Test
    void categoryReplacementRejectsDuplicateTargetsBeforeMutation() {
        prepareActor();
        var request = replacement(List.of(
                new CreateCategory("A", new BigDecimal("10000"), List.of(9L)),
                new CreateCategory("B", new BigDecimal("20000"), List.of(9L))));

        assertThatThrownBy(() -> service.updateFeeItem(1L, 7L, 201L, request))
                .isInstanceOfSatisfying(GeneralException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(FinanceErrorCode.DUPLICATE_TARGET));
        verifyNoInteractions(transactions, audits);
    }

    @Test
    void metadataOnlyEditKeepsTargetsEvenWithPaymentHistory() {
        prepareActor();
        when(actor.getId()).thenReturn(9L);
        var request = new UpdateFeeItemRequest();
        request.setTitle("수정된 회비");
        when(categories.countByFeeItemId(201L)).thenReturn(1L);
        when(targets.findAllByFeeItemId(201L)).thenReturn(List.of(new FeeTarget(201L, 1L, 9L, new BigDecimal("40000"))));

        var result = service.updateFeeItem(1L, 7L, 201L, request);

        assertThat(result.title()).isEqualTo("수정된 회비");
        assertThat(result.expectedAmount()).isEqualByComparingTo("40000");
        verifyNoInteractions(transactions);
        verify(targets, never()).deleteAll(any());
    }

    @Test
    void unpaidCategoriesAndTargetsAreReplacedTogether() {
        prepareActor();
        when(actor.getId()).thenReturn(9L);
        prepareValidMember(9L);
        var request = replacement(List.of(new CreateCategory("새 회비", new BigDecimal("50000"), List.of(9L))));
        var oldCategory = new FeeCategory(201L, "기존", new BigDecimal("40000"), 0);
        ReflectionTestUtils.setField(oldCategory, "id", 1L);
        var oldTarget = new FeeTarget(201L, 1L, 9L, new BigDecimal("40000"));
        var newCategory = new FeeCategory(201L, "새 회비", new BigDecimal("50000"), 0);
        ReflectionTestUtils.setField(newCategory, "id", 2L);
        var newTarget = new FeeTarget(201L, 2L, 9L, new BigDecimal("50000"));
        when(categories.findAllByFeeItemIdOrderByDisplayOrderAsc(201L))
                .thenReturn(List.of(oldCategory), List.of(oldCategory), List.of(newCategory));
        when(targets.findAllByFeeItemId(201L))
                .thenReturn(List.of(oldTarget), List.of(oldTarget), List.of(newTarget), List.of(newTarget));
        when(categories.save(any(FeeCategory.class))).thenReturn(newCategory);
        when(targets.save(any(FeeTarget.class))).thenReturn(newTarget);
        when(categories.countByFeeItemId(201L)).thenReturn(1L);

        var result = service.updateFeeItem(1L, 7L, 201L, request);

        assertThat(result.expectedAmount()).isEqualByComparingTo("50000");
        verify(targets).deleteAll(List.of(oldTarget));
        verify(categories).deleteAll(List.of(oldCategory));
        verify(targets).flush();
        verify(categories).flush();
        verify(targets).save(argThat(target -> target.getFeeCategoryId().equals(2L)
                && target.getMembershipId().equals(9L) && target.getAmountDue().compareTo(new BigDecimal("50000")) == 0));
    }

    private UpdateFeeItemRequest replacement(List<CreateCategory> value) {
        var request = new UpdateFeeItemRequest();
        request.setTitle("새 회비");
        request.setCategories(value);
        return request;
    }

    private void prepareActor() {
        actor = mock(Membership.class);
        when(access.requireStaff(1L, 7L)).thenReturn(actor);
        when(items.findLockedByIdAndOrganizationId(201L, 1L)).thenReturn(Optional.of(item));
    }

    private void prepareValidMember(Long id) {
        Membership target = mock(Membership.class);
        Organization organization = mock(Organization.class);
        when(target.getId()).thenReturn(id);
        when(target.getStatus()).thenReturn(MembershipStatus.ACTIVE);
        when(target.getOrganization()).thenReturn(organization);
        when(organization.getId()).thenReturn(1L);
        when(members.findAllByIdIn(any())).thenReturn(List.of(target));
    }
}
