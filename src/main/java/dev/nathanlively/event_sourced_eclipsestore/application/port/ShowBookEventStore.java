package dev.nathanlively.event_sourced_eclipsestore.application.port;

import dev.nathanlively.event_sourced_eclipsestore.adapter.out.store.EclipseStoreEventStore;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBook;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookEvent;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookId;

public interface ShowBookEventStore extends EventStore<ShowBookId, ShowBookEvent, ShowBook> {

    static ShowBookEventStore createNull() {
        return EclipseStoreEventStore.createNull();
    }

    static ShowBookEventStore createNull(StoreOptions options) {
        return EclipseStoreEventStore.createNull(options);
    }

    sealed interface StoreOptions {
        record WithException(RuntimeException exceptionToThrow) implements StoreOptions {
        }
    }
}
