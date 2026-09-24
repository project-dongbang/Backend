package com.dongbang.event.application.command;

import com.dongbang.event.domain.ParticipantAction;
import java.util.List;

public record ChangeParticipantsCommand(long participantVersion, List<Change> changes) {
    public record Change(Long membershipId, ParticipantAction action) {}
}
