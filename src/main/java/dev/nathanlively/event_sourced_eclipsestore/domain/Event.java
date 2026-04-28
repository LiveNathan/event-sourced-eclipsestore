package dev.nathanlively.event_sourced_eclipsestore.domain;

public interface Event {
    Long eventSequence();
}
