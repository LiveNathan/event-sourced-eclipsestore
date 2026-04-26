package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Inspired by https://github.com/jitterted/jitterticket-event-sourced
class ShowBookTest {

    @Nested
    class CommandsGenerateEvents {

        @Test
        void createShowBookGeneratesShowBookCreated() {
            ShowBookId showBookId = ShowBookId.createRandom();

            ShowBook showBook = ShowBook.create(showBookId, "show book name");

            assertThat(showBook.uncommittedEvents())
                    .containsExactly(
                            new ShowBookCreated(showBookId, null, "show book name")
                    );
        }

    }

    @Nested
    class EventsProjectState {

        @Test
        void showBookCreatedUpdatesName() {
            ShowBookId showBookId = ShowBookId.createRandom();
            ShowBookCreated showBookCreated = new ShowBookCreated(showBookId, 0L, "showBook name");

            ShowBook showBook = ShowBook.reconstitute(List.of(showBookCreated));

            assertThat(showBook.getId())
                    .isEqualTo(showBookId);
            assertThat(showBook.name())
                    .isEqualTo("showBook name");
        }

    }

}