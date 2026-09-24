package com.dongbang.finance.infrastructure.adapter;

import com.dongbang.finance.domain.FeeTargetStatus;
import com.dongbang.finance.domain.repository.FeeTargetRepository;
import com.dongbang.mypage.application.port.MyActivityFeePort;
import com.dongbang.mypage.domain.MyFeeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MyPageFeeActivityAdapter implements MyActivityFeePort {

    private final FeeTargetRepository feeTargetRepository;

    @Override
    public MyFeeStatus getFeeStatus(Long membershipId) {
        var targets = feeTargetRepository.findAllByMembershipId(membershipId);
        if (targets.isEmpty()) {
            return MyFeeStatus.NOT_APPLICABLE;
        }
        return targets.stream().anyMatch(target -> target.getStatus() == FeeTargetStatus.UNPAID)
                ? MyFeeStatus.UNPAID
                : MyFeeStatus.PAID;
    }
}
