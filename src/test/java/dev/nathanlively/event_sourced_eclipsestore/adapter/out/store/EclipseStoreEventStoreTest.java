package dev.nathanlively.event_sourced_eclipsestore.adapter.out.store;

import dev.nathanlively.event_sourced_eclipsestore.application.Checkpoint;
import dev.nathanlively.event_sourced_eclipsestore.domain.Event;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EclipseStoreEventStoreTest {

    private EclipseStoreEventStore storeThatThrows(RuntimeException exception) {
        return EclipseStoreEventStore.createNull(
                new EclipseStoreEventStore.StoreOptions.WithException(exception));
    }

    @Nested
    class HappyPath {

        @Test
        void findByIdReturnsSavedShowBook() {
            ShowBookId showBookId = ShowBookId.createRandom();
            ShowBook showBook = ShowBook.create(showBookId, "show book name");
            EclipseStoreEventStore store = EclipseStoreEventStore.createNull();

            store.save(showBook);

            Optional<ShowBook> found = store.findById(showBookId);

            assertThat(found)
                    .as("Saved show book should be findable by its ID")
                    .isPresent()
                    .hasValueSatisfying(book -> ShowBookAssert.assertThat(book)
                            .hasId(showBookId));
        }

        @Test
        void eventsForAggregateReturnsEventsInSequenceOrder() {
            ShowBookId showBookId = ShowBookId.createRandom();
            ShowBook showBook = ShowBook.create(showBookId, "original name");
            showBook.rename("renamed");
            showBook.delete();
            EclipseStoreEventStore store = EclipseStoreEventStore.createNull();

            store.save(showBook);

            List<ShowBookEvent> events = store.eventsForAggregate(showBookId);

            assertThat(events)
                    .as("Events for an aggregate should come back in the order they were appended")
                    .hasSize(3)
                    .hasExactlyElementsOfTypes(ShowBookCreated.class, ShowBookNameUpdated.class, ShowBookDeleted.class);

            assertThat(events)
                    .as("Event sequences should be strictly increasing")
                    .extracting(ShowBookEvent::eventSequence)
                    .isSorted();
        }

        @Test
        void eventsForAggregateIsolatesEventsByAggregateId() {
            ShowBookId firstId = ShowBookId.createRandom();
            ShowBookId secondId = ShowBookId.createRandom();
            ShowBook first = ShowBook.create(firstId, "irrelevant");
            ShowBook second = ShowBook.create(secondId, "irrelevant");
            second.rename("irrelevant");
            EclipseStoreEventStore store = EclipseStoreEventStore.createNull();

            store.save(first);
            store.save(second);

            assertThat(store.eventsForAggregate(firstId))
                    .as("Events for the first aggregate should not include events from another aggregate")
                    .hasSize(1)
                    .allSatisfy(event -> assertThat(event.showBookId()).isEqualTo(firstId));

            assertThat(store.eventsForAggregate(secondId))
                    .as("Events for the second aggregate should not include events from another aggregate")
                    .hasSize(2)
                    .allSatisfy(event -> assertThat(event.showBookId()).isEqualTo(secondId));
        }

        @Test
        void allEventsAfterReturnsAllEventsAcrossAggregatesInSequenceOrder() {
            ShowBook first = ShowBook.create(ShowBookId.createRandom(), "first");
            ShowBook second = ShowBook.create(ShowBookId.createRandom(), "second");
            EclipseStoreEventStore store = EclipseStoreEventStore.createNull();

            store.save(first);
            store.save(second);

            List<ShowBookEvent> events = store.allEventsAfter(Checkpoint.INITIAL, Set.of()).toList();

            assertThat(events)
                    .as("allEventsAfter with INITIAL checkpoint and no type filter returns every saved event")
                    .hasSize(2)
                    .extracting(ShowBookEvent::eventSequence)
                    .isSorted();
        }

        @Test
        void allEventsAfterExcludesEventsAtOrBeforeCheckpoint() {
            ShowBook first = ShowBook.create(ShowBookId.createRandom(), "first");
            ShowBook second = ShowBook.create(ShowBookId.createRandom(), "second");
            EclipseStoreEventStore store = EclipseStoreEventStore.createNull();
            store.save(first);
            store.save(second);
            long firstEventSequence = store.eventsForAggregate(first.id()).getFirst().eventSequence();

            List<ShowBookEvent> events = store.allEventsAfter(Checkpoint.of(firstEventSequence), Set.of()).toList();

            assertThat(events)
                    .as("Checkpoint should exclude events with sequence <= the checkpoint value")
                    .hasSize(1)
                    .allSatisfy(event -> assertThat(event.eventSequence()).isGreaterThan(firstEventSequence));
        }

        @Test
        void allEventsAfterFiltersByDesiredEventTypes() {
            ShowBook showBook = ShowBook.create(ShowBookId.createRandom(), "original");
            showBook.rename("renamed");
            showBook.delete();
            EclipseStoreEventStore store = EclipseStoreEventStore.createNull();
            store.save(showBook);

            Set<Class<? extends Event>> onlyRenames = Set.of(ShowBookNameUpdated.class);
            List<ShowBookEvent> events = store.allEventsAfter(Checkpoint.INITIAL, onlyRenames).toList();

            assertThat(events)
                    .as("Only the requested event types should be returned")
                    .hasSize(1)
                    .hasOnlyElementsOfType(ShowBookNameUpdated.class);
        }
    }

    @Nested
    class Nullability {

        @Test
        void findByIdReturnsEmptyForUnknownId() {
            EclipseStoreEventStore store = EclipseStoreEventStore.createNull();

            Optional<ShowBook> found = store.findById(ShowBookId.createRandom());

            assertThat(found)
                    .as("Searching for a non-existent show book ID should return an empty Optional")
                    .isEmpty();
        }
    }

    @Nested
    class FailurePaths {

        @Test
        void saveRethrowsConfiguredException() {
            RuntimeException boom = new RuntimeException("storage unavailable");
            EclipseStoreEventStore store = storeThatThrows(boom);
            ShowBook showBook = ShowBookFactory.createDummy();

            assertThatThrownBy(() -> store.save(showBook))
                    .as("Save should throw the configured exception when storage is unavailable")
                    .isEqualTo(boom);
        }

        @Test
        void findByIdRethrowsConfiguredException() {
            RuntimeException boom = new RuntimeException("storage unavailable");
            EclipseStoreEventStore store = storeThatThrows(boom);

            assertThatThrownBy(() -> store.findById(ShowBookId.createRandom()))
                    .as("FindById should throw the configured exception when storage is unavailable")
                    .isEqualTo(boom);
        }

        @Test
        void allEventsAfterRethrowsConfiguredException() {
            RuntimeException boom = new RuntimeException("storage unavailable");
            EclipseStoreEventStore store = storeThatThrows(boom);

            assertThatThrownBy(() -> store.allEventsAfter(Checkpoint.INITIAL, Set.of()))
                    .as("allEventsAfter should throw the configured exception when storage is unavailable")
                    .isEqualTo(boom);
        }
    }
}
