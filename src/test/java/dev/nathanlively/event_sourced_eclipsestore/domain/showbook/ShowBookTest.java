package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

// Inspired by https://github.com/jitterted/jitterticket-event-sourced
class ShowBookTest {

    @Nested
    class CommandsGenerateEvents {

        @Test
        void createShowBookGeneratesShowBookCreated() {
            ShowBook showBook = ShowBook.create("show book name");

            assertThat(showBook.uncommittedEvents())
                    .containsExactly(
                            new ShowBookCreated(showBook.getId(), "show book name")
                    );
        }

        @Test
        void renameShowBookGeneratesShowBookNameUpdated() {
            ShowBook showBook = ShowBook.create("original name");

            showBook.rename("new name");

            assertThat(showBook.uncommittedEvents())
                    .containsExactly(
                            new ShowBookCreated(showBook.getId(), "original name"),
                            new ShowBookNameUpdated(showBook.getId(), "new name")
                    );
        }

        @Test
        void deleteShowBookGeneratesShowBookDeleted() {
            ShowBook showBook = ShowBook.create("name");

            showBook.delete();

            assertThat(showBook.uncommittedEvents())
                    .containsExactly(
                            new ShowBookCreated(showBook.getId(), "name"),
                            new ShowBookDeleted(showBook.getId())
                    );
        }

    }

    @Nested
    class GuardClauses {

        @Test
        void createWithNullNameThrowsException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ShowBook.create(null))
                    .withMessage("Name must not be null or blank.");
        }

        @Test
        void createWithBlankNameThrowsException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ShowBook.create("   "))
                    .withMessage("Name must not be null or blank.");
        }

        @Test
        void renameWithNullNameThrowsException() {
            ShowBook showBook = ShowBook.create("original");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> showBook.rename(null))
                    .withMessage("Name must not be null or blank.");
        }

        @Test
        void renameWithBlankNameThrowsException() {
            ShowBook showBook = ShowBook.create("original");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> showBook.rename("  "))
                    .withMessage("Name must not be null or blank.");
        }

        @Test
        void renameDeletedShowBookThrowsException() {
            ShowBook showBook = ShowBook.create("name");
            showBook.delete();

            assertThatIllegalStateException()
                    .isThrownBy(() -> showBook.rename("new name"))
                    .withMessage("Cannot rename a deleted ShowBook.");
        }

        @Test
        void deleteDeletedShowBookThrowsException() {
            ShowBook showBook = ShowBook.create("name");
            showBook.delete();

            assertThatIllegalStateException()
                    .isThrownBy(showBook::delete)
                    .withMessage("ShowBook is already deleted.");
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

        @Test
        void showBookNameUpdatedUpdatesName() {
            ShowBookId showBookId = ShowBookId.createRandom();
            ShowBookCreated created = new ShowBookCreated(showBookId, 0L, "original");
            ShowBookNameUpdated updated = new ShowBookNameUpdated(showBookId, 1L, "updated");

            ShowBook showBook = ShowBook.reconstitute(List.of(created, updated));

            assertThat(showBook.name())
                    .isEqualTo("updated");
        }

        @Test
        void showBookDeletedUpdatesDeletedFlag() {
            ShowBookId showBookId = ShowBookId.createRandom();
            ShowBookCreated created = new ShowBookCreated(showBookId, 0L, "name");
            ShowBookDeleted deleted = new ShowBookDeleted(showBookId, 1L);

            ShowBook showBook = ShowBook.reconstitute(List.of(created, deleted));

            assertThat(showBook.isDeleted())
                    .isTrue();
        }

    }

}