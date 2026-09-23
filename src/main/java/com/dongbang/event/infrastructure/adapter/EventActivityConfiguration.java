package com.dongbang.event.infrastructure.adapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
@Configuration(proxyBeanMethods = false)
public class EventActivityConfiguration {
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock eventClock() { return Clock.systemUTC(); }
}
