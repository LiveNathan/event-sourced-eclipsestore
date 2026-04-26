package dev.nathanlively.event_sourced_eclipsestore.application;

import dev.nathanlively.event_sourced_eclipsestore.domain.Event;

import java.util.stream.Stream;

public interface EventStreamConsumer {
    void handle(Stream<? extends Event> eventStream);
}
