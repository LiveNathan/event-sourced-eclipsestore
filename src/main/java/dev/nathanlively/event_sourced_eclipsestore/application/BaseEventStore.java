package dev.nathanlively.event_sourced_eclipsestore.application;

import dev.nathanlively.event_sourced_eclipsestore.application.port.EventStore;
import dev.nathanlively.event_sourced_eclipsestore.domain.Event;
import dev.nathanlively.event_sourced_eclipsestore.domain.EventSourcedAggregate;
import dev.nathanlively.event_sourced_eclipsestore.domain.Id;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

// Inspired by https://github.com/jitterted/jitterticket-event-sourced
public abstract class BaseEventStore<ID extends Id, EVENT extends Event, AGGREGATE extends EventSourcedAggregate<EVENT, ID>>
        implements EventStore<ID, EVENT, AGGREGATE> {

    protected final Function<List<EVENT>, AGGREGATE> eventsToAggregate;
    private final List<EventStreamConsumer> eventStreamConsumers = new ArrayList<>();
    private final Map<EventStreamConsumer, Set<Class<? extends Event>>> consumersToDesiredEvents = new HashMap<>();

    protected BaseEventStore(Function<List<EVENT>, AGGREGATE> eventsToAggregate) {
        this.eventsToAggregate = eventsToAggregate;
    }

    @Override
    public void save(AGGREGATE aggregate) {
        ID aggregateId = aggregate.id();
        if (aggregateId == null) {
            throw new IllegalArgumentException("The Aggregate " + aggregate + " must have an ID");
        }
        Stream<EVENT> uncommittedEvents = aggregate.uncommittedEvents();
        List<EVENT> savedEvents = append(aggregateId, uncommittedEvents).toList();
        aggregate.markEventsCommitted();
        notifyConsumers(savedEvents);
    }

    protected abstract Stream<EVENT> append(ID aggregateId, Stream<EVENT> uncommittedEvents);

    private void notifyConsumers(List<EVENT> savedEvents) {
        eventStreamConsumers.forEach(consumer -> {
            Set<Class<? extends Event>> desiredEventClasses = consumersToDesiredEvents.get(consumer);
            if (desiredEventClasses == null) {
                throw new IllegalStateException("No desired events defined for " + consumer);
            }
            Predicate<EVENT> matches = event -> desiredEventClasses.contains(event.getClass());
            List<EVENT> desired = savedEvents.stream().filter(matches).toList();
            if (!desired.isEmpty()) {
                consumer.handle(desired.stream());
            }
        });
    }

    protected abstract List<EVENT> eventsFor(ID id);

    @Override
    public List<EVENT> eventsForAggregate(ID id) {
        return eventsFor(id);
    }

    @Override
    public void subscribe(EventStreamConsumer eventStreamConsumer, Set<Class<? extends Event>> desiredEvents) {
        eventStreamConsumers.add(eventStreamConsumer);
        consumersToDesiredEvents.put(eventStreamConsumer, desiredEvents);
    }

    @Override
    public Optional<AGGREGATE> findById(ID id) {
        List<EVENT> events = eventsForAggregate(id);
        return events.isEmpty()
                ? Optional.empty()
                : Optional.of(eventsToAggregate.apply(events));
    }
}
