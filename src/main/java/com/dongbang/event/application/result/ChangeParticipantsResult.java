package com.dongbang.event.application.result;

public record ChangeParticipantsResult(Long eventId, int addedCount, int removedCount,
                                       int participantCount, Integer capacity, long participantVersion) {}
