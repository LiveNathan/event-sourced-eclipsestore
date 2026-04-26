package dev.nathanlively.event_sourced_eclipsestore.domain;

import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookCreated;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventSourcedAggregateTest {
    @Test
    void eventsAreAppliedUponBeingEnqueued() {
        var eventSourcedAggregate = new EventSourcedAggregate<>() {
            private Event appliedEvent;

            @Override
            protected void apply(Event event) {
                appliedEvent = event;
            }
        };

        ShowBookCreated event = new ShowBookCreated(ShowBookId.createRandom(), 0L, "name");
        eventSourcedAggregate.enqueue(event);

        assertThat(eventSourcedAggregate.appliedEvent)
                .isEqualTo(event);
    }
}