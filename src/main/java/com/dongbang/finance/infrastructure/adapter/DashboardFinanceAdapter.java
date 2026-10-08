package com.dongbang.finance.infrastructure.adapter;

import com.dongbang.dashboard.application.port.DashboardFinancePort;
import com.dongbang.finance.domain.repository.FeeTargetRepository;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.organization.application.facade.MembershipSummary;
import com.dongbang.organization.exception.OrganizationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DashboardFinanceAdapter implements DashboardFinancePort {
    private final FeeTargetRepository feeTargets;
    private final MembershipAccessFacade memberships;

    @Override
    public double getFeePaymentRate(Long organizationId) {
        return feeTargets.dashboardTotals(organizationId).rate();
    }

    @Override
    public int getMemberUnpaidFeeCount(Long organizationId, Long userId) {
        Long membershipId = memberships.getMembershipSummary(organizationId, userId)
                .map(MembershipSummary::membershipId)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.MEMBER_REQUIRED));
        return Math.toIntExact(feeTargets.countDashboardUnpaid(organizationId, membershipId));
    }
}
