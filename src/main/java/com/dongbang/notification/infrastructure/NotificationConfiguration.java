package com.dongbang.notification.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class NotificationConfiguration {

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}
