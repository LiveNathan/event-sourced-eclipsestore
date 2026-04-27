package dev.nathanlively.event_sourced_eclipsestore.application;

import dev.nathanlively.event_sourced_eclipsestore.adapter.out.store.EclipseStoreEventStore;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class BaseEventStoreTest {

    @Nested
    class Save {

        @Test
        void saveNotifiesAllSubscribedConsumers() {
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
        void saveFailsIfAggregateIdIsNull() {
            EclipseStoreEventStore eventStore = EclipseStoreEventStore.createNull();
            ShowBook showBook = ShowBookFactory.createDummy();
            showBook.setId(null);

            assertThatIllegalArgumentException()
                    .as("Saving an aggregate with a null ID should throw IllegalArgumentException")
                    .isThrownBy(() -> eventStore.save(showBook))
                    .withMessageContaining("must have an ID");
        }

        @Test
        void savePropagatesStorageException() {
            RuntimeException storeException = new RuntimeException("Store failure");
            EclipseStoreEventStore eventStore = EclipseStoreEventStore.createNull(
                    new EclipseStoreEventStore.StoreOptions.WithException(storeException)
            );

            assertThatThrownBy(() -> eventStore.save(ShowBookFactory.createDummy()))
                    .as("Exceptions from the underlying storage should be propagated")
                    .isSameAs(storeException);
        }

        @Test
        void saveClearsAggregateUncommittedEvents() {
            EclipseStoreEventStore eventStore = EclipseStoreEventStore.createNull();
            ShowBook showBook = ShowBookFactory.createDummy();
            showBook.rename("New Name");

            assertThat(showBook.uncommittedEvents())
                    .as("ShowBook should have uncommitted events before saving")
                    .isNotEmpty();

            eventStore.save(showBook);

            assertThat(showBook.uncommittedEvents())
                    .as("ShowBook should have no uncommitted events after saving")
                    .isEmpty();
        }
    }

    @Nested
    class FindById {

        @Test
        void findByIdReturnsEmptyWhenNotFound() {
            EclipseStoreEventStore eventStore = EclipseStoreEventStore.createNull();

            Optional<ShowBook> result = eventStore.findById(ShowBookId.createRandom());

            assertThat(result)
                    .as("Finding a non-existent aggregate should return an empty Optional")
                    .isEmpty();
        }

        @Test
        void findByIdReturnsExistingAggregate() {
            EclipseStoreEventStore eventStore = EclipseStoreEventStore.createNull();
            ShowBook showBook = ShowBookFactory.createDummy();
            eventStore.save(showBook);

            Optional<ShowBook> result = eventStore.findById(showBook.id());

            assertThat(result)
                    .as("Finding an existing aggregate should return it in an Optional")
                    .isPresent()
                    .get()
                    .extracting(ShowBook::id)
                    .isEqualTo(showBook.id());
        }
    }

    @Nested
    class Subscribe {

        @Test
        void subscribersOnlyReceiveFilteredEvents() {
            EclipseStoreEventStore eventStore = EclipseStoreEventStore.createNull();
            SpyConsumer createdSubscriber = new SpyConsumer(ShowBookCreated.class);
            SpyConsumer renamedSubscriber = new SpyConsumer(ShowBookNameUpdated.class);
            SpyConsumer deletedSubscriber = new SpyConsumer(ShowBookDeleted.class);
            eventStore.subscribe(createdSubscriber, Set.of(ShowBookCreated.class));
            eventStore.subscribe(renamedSubscriber, Set.of(ShowBookNameUpdated.class));
            eventStore.subscribe(deletedSubscriber, Set.of(ShowBookDeleted.class));

            String originalName = "Acoustic Night";
            String newName = "Open Mic Night";
            ShowBook showBook = ShowBook.create(ShowBookId.createRandom(), originalName);
            showBook.rename(newName);

            eventStore.save(showBook);

            createdSubscriber.verifyHandleInvoked();
            renamedSubscriber.verifyHandleInvoked();
            deletedSubscriber.verifyHandleNotInvoked();
        }
    }
}
