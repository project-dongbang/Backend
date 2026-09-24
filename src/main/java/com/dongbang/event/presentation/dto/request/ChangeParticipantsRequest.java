package com.dongbang.event.presentation.dto.request;

import com.dongbang.event.application.command.ChangeParticipantsCommand;
import com.dongbang.event.domain.ParticipantAction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ChangeParticipantsRequest(
        @NotNull @PositiveOrZero Long participantVersion,
        @NotEmpty List<@NotNull @Valid Change> changes) {
    public record Change(@NotNull @Positive Long membershipId, @NotNull ParticipantAction action) {}

    public ChangeParticipantsCommand toCommand() {
        return new ChangeParticipantsCommand(participantVersion, changes.stream()
                .map(c -> new ChangeParticipantsCommand.Change(c.membershipId(), c.action())).toList());
    }
}
