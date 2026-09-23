package com.dongbang.event.application.command;

import com.dongbang.event.domain.EventDetails;
import com.dongbang.event.domain.EventType;

public record CreateEventCommand(EventType type, EventDetails details) {
}
