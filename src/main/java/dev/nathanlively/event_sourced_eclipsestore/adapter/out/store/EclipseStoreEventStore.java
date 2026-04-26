package dev.nathanlively.event_sourced_eclipsestore.adapter.out.store;

import dev.nathanlively.event_sourced_eclipsestore.application.BaseEventStore;
import dev.nathanlively.event_sourced_eclipsestore.application.Checkpoint;
import dev.nathanlively.event_sourced_eclipsestore.domain.Event;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBook;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookEvent;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookId;
import org.eclipse.store.gigamap.types.GigaIterator;
import org.eclipse.store.gigamap.types.GigaMap;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class EclipseStoreEventStore extends BaseEventStore<ShowBookId, ShowBookEvent, ShowBook> {

    private final EmbeddedStorageManager storageManager;
    private final DataRoot dataRoot;
    private final GigaMap<ShowBookEvent> events;

    public EclipseStoreEventStore(EmbeddedStorageManager storageManager, DataRoot dataRoot) {
        super(ShowBook::reconstitute);
        this.storageManager = storageManager;
        this.dataRoot = dataRoot;
        this.events = dataRoot.showBookEvents();
    }

    public static EclipseStoreEventStore create(EmbeddedStorageManager storageManager) {
        return new EclipseStoreEventStore(storageManager, DataRoot.from(storageManager));
    }

    @Override
    protected List<ShowBookEvent> eventsFor(ShowBookId id) {
        List<ShowBookEvent> matching = events.query(DataRoot.SHOW_BOOK_ID_INDEX.is(id.id())).toList();
        matching.sort(Comparator.comparingLong(ShowBookEvent::eventSequence));
        return matching;
    }

    @Override
    public Stream<ShowBookEvent> save(ShowBookId aggregateId, Stream<ShowBookEvent> uncommittedEvents) {
        List<ShowBookEvent> toSave = uncommittedEvents.toList();
        for (ShowBookEvent event : toSave) {
            event.setEventSequence(dataRoot.nextSequence());
        }
        events.addAll(toSave);
        events.store();
        storageManager.store(dataRoot);
        return toSave.stream();
    }

    @Override
    public Stream<ShowBookEvent> allEventsAfter(Checkpoint checkpoint, Set<Class<? extends Event>> desiredEventTypes) {
        List<ShowBookEvent> result = new ArrayList<>();
        try (GigaIterator<ShowBookEvent> it = events.iterator()) {
            while (it.hasNext()) {
                ShowBookEvent event = it.next();
                if (event.eventSequence() > checkpoint.value()
                    && (desiredEventTypes.isEmpty() || desiredEventTypes.contains(event.getClass()))) {
                    result.add(event);
                }
            }
        }
        result.sort(Comparator.comparingLong(ShowBookEvent::eventSequence));
        return result.stream();
    }
}
