package com.dongbang.event.application.result;

public record EventApplicationResult(Long eventId, Long membershipId, boolean participating,
                                     int participantCount, long participantVersion) {}
