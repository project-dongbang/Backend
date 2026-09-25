package com.dongbang.mypage.application.port;

import java.time.Instant;

public record RegisteredEventActivity(Long eventId, String title, Instant startsAt) {
}
