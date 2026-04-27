package dev.nathanlively.event_sourced_eclipsestore.adapter.out.store;

import dev.nathanlively.event_sourced_eclipsestore.application.Checkpoint;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBook;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookAssert;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookEvent;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookId;
import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("database")
class EclipseStoreEventStoreIntegrationTest {

    @TempDir
    Path storageDir;

    @Test
    void allEventsAfterReturnsPersistedEventsAcrossStorageRestart() {
        ShowBook first = ShowBook.create(ShowBookId.createRandom(), "first");
        ShowBook second = ShowBook.create(ShowBookId.createRandom(), "second");

        withStore(store -> {
            store.save(first);
            store.save(second);
        });

        withStore(store -> {
            List<ShowBookEvent> events = store.allEventsAfter(Checkpoint.INITIAL, Set.of()).toList();

            assertThat(events)
                    .as("All saved events should be replayable in sequence order after a storage restart")
                    .hasSize(2)
                    .extracting(ShowBookEvent::eventSequence)
                    .isSorted();
        });
    }

    private void withStore(Consumer<EclipseStoreEventStore> action) {
        try (EmbeddedStorageManager storageManager = EmbeddedStorage.start(storageDir)) {
            action.accept(EclipseStoreEventStore.create(storageManager));
        }
    }

    @Nested
    class SurvivesStorageRestart {

        @Test
        void createdShowBookSurvivesRestart() {
            ShowBookId showBookId = ShowBookId.createRandom();
            String name = "Acoustic Night";
            ShowBook showBook = ShowBook.create(showBookId, name);

            withStore(store -> store.save(showBook));

            withStore(store -> {
                Optional<ShowBook> found = store.findById(showBookId);
                assertThat(found)
                        .as("Saved show book should be findable after storage restart")
                        .isPresent()
                        .hasValueSatisfying(book -> ShowBookAssert.assertThat(book)
                                .hasName(name));
            });
        }

        @Test
        void renamedShowBookSurvivesRestart() {
            ShowBookId showBookId = ShowBookId.createRandom();
            ShowBook showBook = ShowBook.create(showBookId, "original name");
            withStore(store -> store.save(showBook));

            String newName = "Open Mic Night";
            withStore(store -> {
                ShowBook foundBook = store.findById(showBookId).orElseThrow();
                foundBook.rename(newName);
                store.save(foundBook);
            });

            withStore(store -> {
                Optional<ShowBook> found = store.findById(showBookId);
                assertThat(found)
                        .as("Renamed show book should reflect updated name after restart")
                        .isPresent()
                        .hasValueSatisfying(book -> ShowBookAssert.assertThat(book)
                                .hasName(newName));
            });
        }

        @Test
        void deletedShowBookSurvivesRestart() {
            ShowBookId showBookId = ShowBookId.createRandom();
            ShowBook showBook = ShowBook.create(showBookId, "to be deleted");
            withStore(store -> store.save(showBook));

            withStore(store -> {
                ShowBook foundBook = store.findById(showBookId).orElseThrow();
                foundBook.delete();
                store.save(foundBook);
            });

            withStore(store -> {
                Optional<ShowBook> found = store.findById(showBookId);
                assertThat(found)
                        .as("Deleted show book should be marked deleted after restart")
                        .isPresent()
                        .hasValueSatisfying(book -> ShowBookAssert.assertThat(book)
                                .isDeleted());
            });
        }

    }
}
