package dev.nathanlively.event_sourced_eclipsestore.application;

import dev.nathanlively.event_sourced_eclipsestore.domain.Event;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class SpyConsumer implements EventStreamConsumer {

    private final Class<? extends Event> desiredEventClass;
    private boolean handleInvoked = false;

    public SpyConsumer(Class<? extends Event> desiredEventClass) {
        this.desiredEventClass = desiredEventClass;
    }

    @Override
    public void handle(Stream<? extends Event> eventStream) {
        handleInvoked = true;
        assertThat(eventStream)
                .as("Expected only events of type " + desiredEventClass)
                .allMatch((Predicate<Event>) event -> event.getClass().equals(desiredEventClass));
    }

    public void verifyHandleInvoked() {
        assertThat(handleInvoked)
                .as("handle(eventStream) should have been invoked")
                .isTrue();
    }

    public void verifyHandleNotInvoked() {
        assertThat(handleInvoked)
                .as("handle(eventStream) should NOT have been invoked")
                .isFalse();
    }
}
