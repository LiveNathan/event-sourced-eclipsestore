package dev.nathanlively.event_sourced_eclipsestore.domain;

// Inspired by https://github.com/jitterted/jitterticket-event-sourced
public abstract class Event {
    protected Long eventSequence;

    protected Event(Long eventSequence) {
        this.eventSequence = eventSequence;
    }

    public Long eventSequence() {
        return eventSequence;
    }

    public void setEventSequence(Long eventSequence) {
        this.eventSequence = eventSequence;
    }
}
