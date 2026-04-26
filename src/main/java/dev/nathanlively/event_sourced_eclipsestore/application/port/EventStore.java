package dev.nathanlively.event_sourced_eclipsestore.application.port;

import dev.nathanlively.event_sourced_eclipsestore.application.Checkpoint;
import dev.nathanlively.event_sourced_eclipsestore.application.EventStreamConsumer;
import dev.nathanlively.event_sourced_eclipsestore.domain.Event;
import dev.nathanlively.event_sourced_eclipsestore.domain.EventSourcedAggregate;
import dev.nathanlively.event_sourced_eclipsestore.domain.Id;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

// Inspired by https://github.com/jitterted/jitterticket-event-sourced
public interface EventStore<ID extends Id, EVENT extends Event, AGGREGATE extends EventSourcedAggregate<EVENT, ID>> {
    void save(AGGREGATE aggregate);

    /**
     * Saves events associated with the aggregate ID so they can be retrieved later.
     * Does NOT tell subscribers that these events were saved, that happens in the above
     * save(AGGREGATE aggregate) method
     */
    Stream<EVENT> save(ID aggregateId, Stream<EVENT> uncommittedEvents);

    Optional<AGGREGATE> findById(ID id);

    List<EVENT> eventsForAggregate(ID id);

    void subscribe(EventStreamConsumer eventStreamConsumer,
                   Set<Class<? extends Event>> desiredEvents);

    Stream<EVENT> allEventsAfter(Checkpoint checkpoint,
                                 Set<Class<? extends Event>> desiredEventTypes);
}
