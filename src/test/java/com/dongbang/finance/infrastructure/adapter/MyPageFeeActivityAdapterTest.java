package com.dongbang.finance.infrastructure.adapter;

import com.dongbang.finance.domain.FeeTarget;
import com.dongbang.finance.domain.FeeTargetStatus;
import com.dongbang.finance.domain.repository.FeeTargetRepository;
import com.dongbang.mypage.domain.MyFeeStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class MyPageFeeActivityAdapterTest {
    private final FeeTargetRepository repository = mock(FeeTargetRepository.class);
    private final MyPageFeeActivityAdapter adapter = new MyPageFeeActivityAdapter(repository);

    @Test
    void noTargetsMeansNotApplicable() {
        given(repository.findAllByMembershipId(20L)).willReturn(List.of());
        assertThat(adapter.getFeeStatus(20L)).isEqualTo(MyFeeStatus.NOT_APPLICABLE);
    }

    @Test
    void anyUnpaidTargetMakesSummaryUnpaid() {
        FeeTarget paid = new FeeTarget(1L, 2L, 20L, BigDecimal.TEN);
        paid.changeStatus(FeeTargetStatus.PAID, null, 9L, Instant.now());
        FeeTarget unpaid = new FeeTarget(2L, 2L, 20L, BigDecimal.TEN);
        given(repository.findAllByMembershipId(20L)).willReturn(List.of(paid, unpaid));

        assertThat(adapter.getFeeStatus(20L)).isEqualTo(MyFeeStatus.UNPAID);
    }
}
