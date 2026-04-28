package dev.nathanlively.event_sourced_eclipsestore.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

class EventSourcedAggregateTest {

    private static class StubAggregate extends EventSourcedAggregate<StubEvent, StubId> {
        final List<StubEvent> appliedEvents = new ArrayList<>();
        boolean failOnApply = false;

        @Override
        protected void apply(StubEvent event) {
            if (failOnApply) {
                throw new RuntimeException("Apply failed");
            }
            appliedEvents.add(event);
        }
    }

    private record StubEvent(Long eventSequence) implements Event {
    }

    private record StubId(UUID id) implements Id {
    }

    @Nested
    class HappyPath {

        @Test
        void enqueueAppliesEventAndAddsToUncommitted() {
            StubAggregate aggregate = new StubAggregate();
            StubEvent event = new StubEvent(0L);

            aggregate.enqueue(event);

            assertThat(aggregate.appliedEvents)
                    .containsExactly(event);
            assertThat(aggregate.uncommittedEvents())
                    .containsExactly(event);
        }

        @Test
        void applyAllAppliesEveryEventInSequence() {
            StubAggregate aggregate = new StubAggregate();
            List<StubEvent> history = List.of(new StubEvent(0L), new StubEvent(1L));

            aggregate.applyAll(history);

            assertThat(aggregate.appliedEvents)
                    .containsExactlyElementsOf(history);
            assertThat(aggregate.uncommittedEvents())
                    .isEmpty();
        }

        @Test
        void markEventsCommittedClearsUncommittedEvents() {
            StubAggregate aggregate = new StubAggregate();
            aggregate.enqueue(new StubEvent(0L));

            aggregate.markEventsCommitted();

            assertThat(aggregate.uncommittedEvents())
                    .isEmpty();
        }
    }

    @Nested
    class FailurePaths {

        @Test
        void enqueueAtomicityIsMaintainedWhenApplyFails() {
            StubAggregate aggregate = new StubAggregate();
            aggregate.failOnApply = true;
            StubEvent event = new StubEvent(0L);

            assertThatRuntimeException()
                    .isThrownBy(() -> aggregate.enqueue(event));

            assertThat(aggregate.uncommittedEvents())
                    .as("Event should not be in uncommitted list if apply failed")
                    .isEmpty();
        }
    }
}
