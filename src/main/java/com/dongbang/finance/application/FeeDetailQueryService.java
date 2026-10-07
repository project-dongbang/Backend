package com.dongbang.finance.application;

import com.dongbang.finance.domain.FeeTargetStatus;
import com.dongbang.finance.domain.repository.FeeItemRepository;
import com.dongbang.finance.domain.repository.FeeTargetRepository;
import com.dongbang.finance.domain.repository.FeeCategoryRepository;
import com.dongbang.finance.domain.repository.FinancialTransactionRepository;
import com.dongbang.finance.presentation.dto.FinanceDtos.PaymentAccount;
import com.dongbang.finance.exception.FinanceErrorCode;
import com.dongbang.finance.application.FeeDetailResult.*;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.organization.application.facade.OrganizationAccessFacade;
import com.dongbang.organization.domain.MembershipStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeeDetailQueryService {
    private final FeeItemRepository feeItems;
    private final FeeTargetRepository feeTargets;
    private final FeeCategoryRepository feeCategories;
    private final FinancialTransactionRepository transactions;
    private final MembershipAccessFacade memberships;
    private final OrganizationAccessFacade organizations;

    public FeeItemDetail detail(Long organizationId, Long userId, Long feeItemId) {
        organizations.requireActiveOrganization(organizationId);
        var member = memberships.getMembershipSummary(organizationId, userId)
                .filter(value -> value.status() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN));
        var item = feeItems.findByIdAndOrganizationId(feeItemId, organizationId)
                .orElseThrow(() -> new GeneralException(FinanceErrorCode.FEE_ITEM_NOT_FOUND));

        FeeStaffSummary staffSummary = null;
        MyFeePayment myPayment = null;
        FeeEditDetails editDetails = null;
        boolean staff = member.role().isStaff();
        if (staff) {
            var targets = feeTargets.findAllByFeeItemId(feeItemId);
            long paidCount = 0;
            BigDecimal collectedAmount = BigDecimal.ZERO;
            BigDecimal expectedAmount = BigDecimal.ZERO;
            // 카테고리 금액 대신 대상별 부과 금액 기준 집계
            for (var target : targets) {
                expectedAmount = expectedAmount.add(target.getAmountDue());
                if (target.getStatus() == FeeTargetStatus.PAID) {
                    paidCount++;
                    collectedAmount = collectedAmount.add(target.getAmountDue());
                }
            }
            staffSummary = new FeeStaffSummary(targets.size(), paidCount, targets.size() - paidCount,
                    collectedAmount, expectedAmount);
            List<FeeEditCategory> categories = feeCategories.findAllByFeeItemIdOrderByDisplayOrderAsc(feeItemId).stream()
                    .map(category -> new FeeEditCategory(category.getId(), category.getName(), category.getAmount(),
                            targets.stream().filter(target -> category.getId().equals(target.getFeeCategoryId()))
                                    .map(target -> new FeeEditTarget(target.getId(), target.getMembershipId(),
                                            target.getAmountDue(), target.getStatus())).toList()))
                    .toList();
            editDetails = new FeeEditDetails(new PaymentAccount(item.getBankName(), item.getAccountNumber(),
                    item.getAccountHolder()), paidCount == 0 && !transactions.existsByFeeItemId(feeItemId), categories);
        } else {
            myPayment = feeTargets.findByFeeItemIdAndMembershipId(feeItemId, member.membershipId())
                    .map(target -> new MyFeePayment(target.getId(), target.getAmountDue(),
                            target.getStatus(), target.getPaidAt())).orElse(null);
        }
        return new FeeItemDetail(item.getId(), staff ? FeeViewerType.STAFF : FeeViewerType.MEMBER,
                item.getTitle(), item.getDueDate(), item.getDescription(), staffSummary, myPayment, editDetails);
    }
}
