package com.dongbang.event.application.result;

import com.dongbang.event.domain.ParticipantAction;
import java.util.List;

public record ParticipantCandidatesResult(
        Long eventId, Integer capacity, int participantCount, long participantVersion,
        List<Member> members, int page, int size, long totalElements, int totalPages, boolean hasNext) {
    public record Member(Long membershipId, String memberName, String studentNumber,
                         String department, boolean participating, List<ParticipantAction> allowedActions) {}
}
