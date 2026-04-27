package dev.nathanlively.event_sourced_eclipsestore.adapter.out.store;

import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBook;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookAssert;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookFactory;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EclipseStoreEventStoreTest {

    @Nested
    class Retrieval {

        @Test
        void findByIdReturnsUploadedDocument() {
            ShowBookId showBookId = ShowBookId.createRandom();
            ShowBook showBook = ShowBook.create(showBookId, "show book name");
            EclipseStoreEventStore store = EclipseStoreEventStore.createNull();

            store.save(showBook);

            Optional<ShowBook> found = store.findById(showBookId);

            assertThat(found)
                    .as("Saved document should be findable by its ID")
                    .isPresent()
                    .hasValueSatisfying(doc -> ShowBookAssert.assertThat(doc)
                            .hasId(showBookId));
        }
    }

    @Nested
    class Nullability {

        @Test
        void findByIdReturnsEmptyForUnknownId() {
            EclipseStoreEventStore store = EclipseStoreEventStore.createNull();

            Optional<ShowBook> found = store.findById(ShowBookId.createRandom());

            assertThat(found)
                    .as("Searching for a non-existent document ID should return an empty Optional")
                    .isEmpty();
        }

        @Test
        void forcedErrorThrowsOnSave() {
            RuntimeException boom = new RuntimeException("storage unavailable");
            EclipseStoreEventStore store = EclipseStoreEventStore.createNull(
                    new EclipseStoreEventStore.StoreOptions.WithException(boom));
            ShowBook document = ShowBookFactory.createDummy();

            assertThatThrownBy(() -> store.save(document))
                    .as("Save should throw the configured exception when storage is unavailable")
                    .isEqualTo(boom);
        }

        @Test
        void forcedErrorThrowsOnFindById() {
            RuntimeException boom = new RuntimeException("storage unavailable");
            EclipseStoreEventStore store = EclipseStoreEventStore.createNull(
                    new EclipseStoreEventStore.StoreOptions.WithException(boom));

            assertThatThrownBy(() -> store.findById(ShowBookId.createRandom()))
                    .as("FindById should throw the configured exception when storage is unavailable")
                    .isEqualTo(boom);
        }
    }
}
