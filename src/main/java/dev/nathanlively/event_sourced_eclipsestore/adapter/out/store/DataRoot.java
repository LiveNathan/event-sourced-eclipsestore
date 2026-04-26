package dev.nathanlively.event_sourced_eclipsestore.adapter.out.store;

import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookEvent;
import org.eclipse.store.gigamap.types.BinaryIndexerUUID;
import org.eclipse.store.gigamap.types.GigaMap;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;

import java.util.UUID;

public class DataRoot {

    public static final BinaryIndexerUUID<ShowBookEvent> SHOW_BOOK_ID_INDEX = new BinaryIndexerUUID.Abstract<>() {
        @Override
        protected UUID getUUID(ShowBookEvent entity) {
            return entity.showBookId().id();
        }
    };

    private final GigaMap<ShowBookEvent> showBookEvents = GigaMap.<ShowBookEvent>Builder()
            .withBitmapIndex(SHOW_BOOK_ID_INDEX)
            .build();
    private long sequenceCounter = 0L;

    public DataRoot() {
        super();
    }

    public static DataRoot from(EmbeddedStorageManager storageManager) {
        if (storageManager.root() instanceof DataRoot existing) {
            return existing;
        }
        DataRoot fresh = new DataRoot();
        storageManager.setRoot(fresh);
        storageManager.storeRoot();
        return fresh;
    }

    public GigaMap<ShowBookEvent> showBookEvents() {
        return showBookEvents;
    }

    public synchronized long nextSequence() {
        return ++sequenceCounter;
    }
}
