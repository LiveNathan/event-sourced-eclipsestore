package dev.nathanlively.event_sourced_eclipsestore.application;

import dev.nathanlively.event_sourced_eclipsestore.adapter.out.store.EclipseStoreEventStore;
import dev.nathanlively.event_sourced_eclipsestore.domain.Event;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class BaseEventStoreTest {

    @Nested
    class Save {

        @Test
        void saveSendsEventsToMultipleSubscribedConsumers() {
            EclipseStoreEventStore eventStore = EclipseStoreEventStore.createNull();
            AtomicInteger counter = new AtomicInteger(0);
            EventStreamConsumer countingSpy = stream -> {
                //noinspection ResultOfMethodCallIgnored
                stream.toList();
                counter.incrementAndGet();
            };
            eventStore.subscribe(countingSpy, Set.of(ShowBookCreated.class));
            eventStore.subscribe(countingSpy, Set.of(ShowBookCreated.class));

            eventStore.save(ShowBookFactory.createDummy());

            assertThat(counter)
                    .as("Each subscribed consumer should be invoked once")
                    .hasValue(2);
        }

        @Test
        void saveThrowsExceptionWhenAggregateIdIsNull() {
            EclipseStoreEventStore eventStore = EclipseStoreEventStore.createNull();
            ShowBook showBook = ShowBookFactory.createDummy();
            showBook.setId(null);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> eventStore.save(showBook))
                    .withMessageContaining("must have an ID");
        }

        @Test
        void savePropagatesExceptionFromUnderlyingStore() {
            RuntimeException storeException = new RuntimeException("Store failure");
            EclipseStoreEventStore eventStore = EclipseStoreEventStore.createNull(
                    new EclipseStoreEventStore.StoreOptions.WithException(storeException)
            );

            assertThatThrownBy(() -> eventStore.save(ShowBookFactory.createDummy()))
                    .isSameAs(storeException);
        }

        @Test
        void saveClearsUncommittedEventsFromAggregate() {
            EclipseStoreEventStore eventStore = EclipseStoreEventStore.createNull();
            ShowBook showBook = ShowBookFactory.createDummy();
            showBook.rename("New Name");
            assertThat(showBook.uncommittedEvents()).isNotEmpty();

            eventStore.save(showBook);

            assertThat(showBook.uncommittedEvents()).isEmpty();
        }
    }

    @Nested
    class FindById {

        @Test
        void findByIdReturnsEmptyWhenAggregateDoesNotExist() {
            EclipseStoreEventStore eventStore = EclipseStoreEventStore.createNull();

            Optional<ShowBook> result = eventStore.findById(ShowBookId.createRandom());

            assertThat(result).isEmpty();
        }

        @Test
        void findByIdReturnsAggregateWhenItExists() {
            EclipseStoreEventStore eventStore = EclipseStoreEventStore.createNull();
            ShowBook showBook = ShowBookFactory.createDummy();
            eventStore.save(showBook);

            Optional<ShowBook> result = eventStore.findById(showBook.getId());

            assertThat(result)
                    .isPresent()
                    .get()
                    .extracting(ShowBook::getId)
                    .isEqualTo(showBook.getId());
        }
    }

    @Nested
    class Subscribe {

        @Test
        void subscribersOnlyReceiveDesiredEventTypes() {
            EclipseStoreEventStore eventStore = EclipseStoreEventStore.createNull();
            SpyConsumer createdSubscriber = new SpyConsumer(ShowBookCreated.class);
            SpyConsumer renamedSubscriber = new SpyConsumer(ShowBookNameUpdated.class);
            SpyConsumer deletedSubscriber = new SpyConsumer(ShowBookDeleted.class);
            eventStore.subscribe(createdSubscriber, Set.of(ShowBookCreated.class));
            eventStore.subscribe(renamedSubscriber, Set.of(ShowBookNameUpdated.class));
            eventStore.subscribe(deletedSubscriber, Set.of(ShowBookDeleted.class));

            ShowBook showBook = ShowBook.create(ShowBookId.createRandom(), "Acoustic Night");
            showBook.rename("Open Mic Night");
            eventStore.save(showBook);

            createdSubscriber.verifyHandleInvoked();
            renamedSubscriber.verifyHandleInvoked();
            deletedSubscriber.verifyHandleNotInvoked();
        }
    }

    private static class SpyConsumer implements EventStreamConsumer {

        private final Class<? extends Event> desiredEventClass;
        private boolean handleInvoked = false;

        SpyConsumer(Class<? extends Event> desiredEventClass) {
            this.desiredEventClass = desiredEventClass;
        }

        @Override
        public void handle(Stream<? extends Event> eventStream) {
            handleInvoked = true;
            assertThat(eventStream)
                    .as("Expected only events of type " + desiredEventClass)
                    .allMatch((Predicate<Event>) event -> event.getClass().equals(desiredEventClass));
        }

        void verifyHandleInvoked() {
            assertThat(handleInvoked)
                    .as("handle(eventStream) should have been invoked")
                    .isTrue();
        }

        void verifyHandleNotInvoked() {
            assertThat(handleInvoked)
                    .as("handle(eventStream) should NOT have been invoked")
                    .isFalse();
        }
    }
}
