package com.dongbang.mypage.application.port;

import com.dongbang.mypage.domain.MyFeeStatus;

public interface MyActivityFeePort {
    MyFeeStatus getFeeStatus(Long membershipId);
}
