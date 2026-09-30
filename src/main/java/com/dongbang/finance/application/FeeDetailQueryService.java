package com.dongbang.finance.application;

import com.dongbang.finance.domain.FeeTargetStatus;
import com.dongbang.finance.domain.repository.FeeItemRepository;
import com.dongbang.finance.domain.repository.FeeTargetRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeeDetailQueryService {
    private final FeeItemRepository feeItems;
    private final FeeTargetRepository feeTargets;
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
        } else {
            myPayment = feeTargets.findByFeeItemIdAndMembershipId(feeItemId, member.membershipId())
                    .map(target -> new MyFeePayment(target.getId(), target.getAmountDue(),
                            target.getStatus(), target.getPaidAt())).orElse(null);
        }
        return new FeeItemDetail(item.getId(), staff ? FeeViewerType.STAFF : FeeViewerType.MEMBER,
                item.getTitle(), item.getDueDate(), item.getDescription(), staffSummary, myPayment);
    }
}
