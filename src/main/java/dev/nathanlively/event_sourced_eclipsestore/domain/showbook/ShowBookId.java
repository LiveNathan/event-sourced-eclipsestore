package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

import dev.nathanlively.event_sourced_eclipsestore.domain.Id;

import java.util.UUID;

public record ShowBookId(UUID id) implements Id {
    public static ShowBookId createRandom() {
        return new ShowBookId(UUID.randomUUID());
    }
}
