package dev.nathanlively.event_sourced_eclipsestore.adapter.out.store;

import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBook;
import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("database")
class EclipseStoreEventStoreIntegrationTest {

    @TempDir
    Path storageDir;

    @Test
    void persistedShowBookIsRetrievableAfterStorageRestart() {
        ShowBook showBook = ShowBook.create("Acoustic Night");

        try (EmbeddedStorageManager storageManager = EmbeddedStorage.start(storageDir)) {
            EclipseStoreEventStore store = EclipseStoreEventStore.create(storageManager);
            store.save(showBook);
        }

        try (EmbeddedStorageManager storageManager = EmbeddedStorage.start(storageDir)) {
            EclipseStoreEventStore store = EclipseStoreEventStore.create(storageManager);

            Optional<ShowBook> found = store.findById(showBook.getId());

            assertThat(found)
                    .as("Saved show book should be findable after storage restart")
                    .isPresent()
                    .hasValueSatisfying(book -> assertThat(book.name()).isEqualTo("Acoustic Night"));
        }
    }
}
