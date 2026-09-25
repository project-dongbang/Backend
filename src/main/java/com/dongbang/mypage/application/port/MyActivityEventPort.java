package com.dongbang.mypage.application.port;

import java.util.List;

public interface MyActivityEventPort {
    List<RegisteredEventActivity> findRegisteredEvents(Long organizationId, Long membershipId);
}
