package com.dongbang.event.infrastructure.adapter;

import com.dongbang.event.application.port.EventActivity;
import com.dongbang.event.application.port.EventActivityPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class EventActivityConfiguration {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock eventClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean(EventActivityPort.class)
    EventActivityPort temporaryEventActivityPort() {
        // TODO(event, attendance): 참가 등록·출석 구현 시 실제 조회 어댑터로 교체
        // 참가자 수·본인 참가 여부·버전·조기 마감·출석 시작 여부의 임시 값
        // 신청·출석 쓰기 API 활성화 전 교체 필수
        return (organizationId, eventId, userId) ->
                new EventActivity(0, false, 0L, "NOT_STARTED", false);
    }
}
