package dev.nathanlively.event_sourced_eclipsestore.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

// Inspired by https://github.com/jitterted/jitterticket-event-sourced
public abstract class EventSourcedAggregate<EVENT extends Event, ID extends Id> {
    private ID id;
    private final List<EVENT> uncommittedEvents = new ArrayList<>();

    protected void enqueue(EVENT event) {
        uncommittedEvents.add(event);
        apply(event);
    }

    protected void applyAll(List<EVENT> loadedEvents) {
        loadedEvents.forEach(this::apply);
    }

    protected abstract void apply(EVENT event);

    public Stream<EVENT> uncommittedEvents() {
        return uncommittedEvents.stream();
    }

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }

}
