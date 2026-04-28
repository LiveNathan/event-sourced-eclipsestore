package dev.nathanlively.event_sourced_eclipsestore.adapter.out.store;

import dev.nathanlively.event_sourced_eclipsestore.application.BaseEventStore;
import dev.nathanlively.event_sourced_eclipsestore.application.Checkpoint;
import dev.nathanlively.event_sourced_eclipsestore.domain.Event;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBook;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookEvent;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookId;
import org.eclipse.store.gigamap.types.GigaMap;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class EclipseStoreEventStore extends BaseEventStore<ShowBookId, ShowBookEvent, ShowBook> {

    private final Storage storage;

    private EclipseStoreEventStore(Storage storage) {
        super(ShowBook::reconstitute);
        this.storage = storage;
    }

    public static EclipseStoreEventStore create(EmbeddedStorageManager storageManager) {
        return new EclipseStoreEventStore(new EclipseStorage(storageManager, DataRoot.from(storageManager)));
    }

    public static EclipseStoreEventStore createNull() {
        return new EclipseStoreEventStore(new InMemoryStorage());
    }

    public static EclipseStoreEventStore createNull(StoreOptions options) {
        return switch (options) {
            case StoreOptions.WithException(var e) -> new EclipseStoreEventStore(new ThrowingStorage(e));
        };
    }

    @Override
    protected Stream<ShowBookEvent> append(ShowBookId aggregateId, Stream<ShowBookEvent> uncommittedEvents) {
        return storage.append(aggregateId, uncommittedEvents);
    }

    @Override
    protected List<ShowBookEvent> eventsFor(ShowBookId id) {
        return storage.eventsFor(id);
    }

    public sealed interface StoreOptions {
        record WithException(RuntimeException exceptionToThrow) implements StoreOptions {
        }
    }

    @Override
    public Stream<ShowBookEvent> allEventsAfter(Checkpoint checkpoint, Set<Class<? extends Event>> desiredEventTypes) {
        return storage.allEventsAfter(checkpoint, desiredEventTypes);
    }

    private interface Storage {
        Stream<ShowBookEvent> append(ShowBookId id, Stream<ShowBookEvent> uncommittedEvents);

        List<ShowBookEvent> eventsFor(ShowBookId id);

        Stream<ShowBookEvent> allEventsAfter(Checkpoint checkpoint, Set<Class<? extends Event>> desiredEventTypes);
    }

    private record EclipseStorage(EmbeddedStorageManager storageManager, DataRoot dataRoot) implements Storage {

        @Override
        public Stream<ShowBookEvent> append(ShowBookId id, Stream<ShowBookEvent> uncommittedEvents) {
            GigaMap<ShowBookEvent> events = dataRoot.showBookEvents();
            List<ShowBookEvent> toSave = uncommittedEvents
                    .map(event -> event.withSequence(dataRoot.nextSequence()))
                    .toList();
            events.addAll(toSave);
            events.store();
            storageManager.store(dataRoot);
            return toSave.stream();
        }

        @Override
        public List<ShowBookEvent> eventsFor(ShowBookId id) {
            List<ShowBookEvent> matching = dataRoot.showBookEvents()
                    .query(DataRoot.SHOW_BOOK_ID_INDEX.is(id.id()))
                    .toList();
            matching.sort(Comparator.comparingLong(ShowBookEvent::eventSequence));
            return matching;
        }

        @Override
        public Stream<ShowBookEvent> allEventsAfter(Checkpoint checkpoint, Set<Class<? extends Event>> desiredEventTypes) {
            List<ShowBookEvent> result = dataRoot.showBookEvents()
                    .query(DataRoot.EVENT_SEQUENCE_INDEX.greaterThan(checkpoint.value()))
                    .toList();
            if (!desiredEventTypes.isEmpty()) {
                result.removeIf(event -> !desiredEventTypes.contains(event.getClass()));
            }
            result.sort(Comparator.comparingLong(ShowBookEvent::eventSequence));
            return result.stream();
        }
    }

    private record ThrowingStorage(RuntimeException exception) implements Storage {
        @Override
        public Stream<ShowBookEvent> append(ShowBookId id, Stream<ShowBookEvent> uncommittedEvents) {
            throw exception;
        }

        @Override
        public List<ShowBookEvent> eventsFor(ShowBookId id) {
            throw exception;
        }

        @Override
        public Stream<ShowBookEvent> allEventsAfter(Checkpoint checkpoint, Set<Class<? extends Event>> desiredEventTypes) {
            throw exception;
        }
    }

    private static class InMemoryStorage implements Storage {
        private final List<ShowBookEvent> events = new ArrayList<>();
        private final AtomicLong sequenceCounter = new AtomicLong(0L);

        @Override
        public Stream<ShowBookEvent> append(ShowBookId id, Stream<ShowBookEvent> uncommittedEvents) {
            List<ShowBookEvent> toSave = uncommittedEvents
                    .map(event -> event.withSequence(sequenceCounter.incrementAndGet()))
                    .toList();
            events.addAll(toSave);
            return toSave.stream();
        }

        @Override
        public List<ShowBookEvent> eventsFor(ShowBookId id) {
            return events.stream()
                    .filter(e -> e.showBookId().equals(id))
                    .sorted(Comparator.comparingLong(ShowBookEvent::eventSequence))
                    .toList();
        }

        @Override
        public Stream<ShowBookEvent> allEventsAfter(Checkpoint checkpoint, Set<Class<? extends Event>> desiredEventTypes) {
            return events.stream()
                    .filter(e -> e.eventSequence() > checkpoint.value())
                    .filter(e -> desiredEventTypes.isEmpty() || desiredEventTypes.contains(e.getClass()))
                    .sorted(Comparator.comparingLong(ShowBookEvent::eventSequence));
        }
    }
}
