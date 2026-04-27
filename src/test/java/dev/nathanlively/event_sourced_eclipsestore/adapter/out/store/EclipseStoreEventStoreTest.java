package dev.nathanlively.event_sourced_eclipsestore.adapter.out.store;

import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBook;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EclipseStoreEventStoreTest {

    @Nested
    class Retrieval {

        @Test
        void findByIdReturnsUploadedDocument() {
            ShowBook showBook = ShowBook.create("show book name");
            EclipseStoreEventStore store = EclipseStoreEventStore.createNull();

            store.save(showBook);

            Optional<ShowBook> found = store.findById(showBook.getId());

            assertThat(found)
                    .as("Saved document should be findable by its ID")
                    .isPresent()
                    .hasValueSatisfying(doc -> ShowBookAssert.assertThat(doc)
                            .hasId(showBook.getId()));
        }
    }
}