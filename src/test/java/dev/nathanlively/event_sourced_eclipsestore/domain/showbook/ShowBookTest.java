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
            ShowBookId showBookId = ShowBookId.createRandom();
            String showBookName = "show book name";

            ShowBook showBook = ShowBook.create(showBookId, showBookName);

            assertThat(showBook.uncommittedEvents())
                    .as("Creating a ShowBook should generate a ShowBookCreated event")
                    .containsExactly(
                            new ShowBookCreated(showBookId, showBookName)
                    );
        }

        @Test
        void renameShowBookGeneratesShowBookNameUpdated() {
            ShowBookId showBookId = ShowBookId.createRandom();
            String originalName = "original name";
            String newName = "new name";
            ShowBook showBook = ShowBook.create(showBookId, originalName);

            showBook.rename(newName);

            assertThat(showBook.uncommittedEvents())
                    .as("Renaming a ShowBook should generate a ShowBookNameUpdated event")
                    .containsExactly(
                            new ShowBookCreated(showBookId, originalName),
                            new ShowBookNameUpdated(showBookId, newName)
                    );
        }

        @Test
        void deleteShowBookGeneratesShowBookDeleted() {
            ShowBookId showBookId = ShowBookId.createRandom();
            String name = "name";
            ShowBook showBook = ShowBook.create(showBookId, name);

            showBook.delete();

            assertThat(showBook.uncommittedEvents())
                    .as("Deleting a ShowBook should generate a ShowBookDeleted event")
                    .containsExactly(
                            new ShowBookCreated(showBookId, name),
                            new ShowBookDeleted(showBookId)
                    );
        }

    }

    @Nested
    class GuardClauses {

        @Test
        void createWithBlankNameThrowsException() {
            assertThatIllegalArgumentException()
                    .as("Creating a ShowBook with a blank name should throw an IllegalArgumentException")
                    .isThrownBy(() -> ShowBook.create(ShowBookId.createRandom(), "   "))
                    .withMessage("Name must not be null or blank.");
        }

        @Test
        void renameWithBlankNameThrowsException() {
            ShowBook showBook = ShowBook.create(ShowBookId.createRandom(), "original");

            assertThatIllegalArgumentException()
                    .as("Renaming a ShowBook with a blank name should throw an IllegalArgumentException")
                    .isThrownBy(() -> showBook.rename("  "))
                    .withMessage("Name must not be null or blank.");
        }

        @Test
        void renameDeletedShowBookThrowsException() {
            ShowBook showBook = ShowBook.create(ShowBookId.createRandom(), "name");
            showBook.delete();

            assertThatIllegalStateException()
                    .as("Renaming a deleted ShowBook should throw an IllegalStateException")
                    .isThrownBy(() -> showBook.rename("new name"))
                    .withMessage("Cannot rename a deleted ShowBook.");
        }

        @Test
        void deleteDeletedShowBookThrowsException() {
            ShowBook showBook = ShowBook.create(ShowBookId.createRandom(), "name");
            showBook.delete();

            assertThatIllegalStateException()
                    .as("Deleting an already deleted ShowBook should throw an IllegalStateException")
                    .isThrownBy(showBook::delete)
                    .withMessage("ShowBook is already deleted.");
        }

    }

    @Nested
    class EventsProjectState {

        @Test
        void showBookCreatedUpdatesName() {
            ShowBookId showBookId = ShowBookId.createRandom();
            String showBookName = "showBook name";
            ShowBookCreated showBookCreated = new ShowBookCreated(showBookId, 0L, showBookName);

            ShowBook showBook = ShowBook.reconstitute(List.of(showBookCreated));

            ShowBookAssert.assertThat(showBook)
                    .as("Reconstituted ShowBook should have the ID from the event")
                    .hasId(showBookId);
            assertThat(showBook.name())
                    .as("Reconstituted ShowBook should have the name from the event")
                    .isEqualTo(showBookName);
        }

        @Test
        void showBookNameUpdatedUpdatesName() {
            ShowBookId showBookId = ShowBookId.createRandom();
            String updatedName = "updated";
            ShowBookCreated created = new ShowBookCreated(showBookId, 0L, "original");
            ShowBookNameUpdated updated = new ShowBookNameUpdated(showBookId, 1L, updatedName);

            ShowBook showBook = ShowBook.reconstitute(List.of(created, updated));

            assertThat(showBook.name())
                    .as("Reconstituted ShowBook should have the updated name")
                    .isEqualTo(updatedName);
        }

        @Test
        void showBookDeletedUpdatesDeletedFlag() {
            ShowBookId showBookId = ShowBookId.createRandom();
            ShowBookCreated created = new ShowBookCreated(showBookId, 0L, "name");
            ShowBookDeleted deleted = new ShowBookDeleted(showBookId, 1L);

            ShowBook showBook = ShowBook.reconstitute(List.of(created, deleted));

            assertThat(showBook.isDeleted())
                    .as("Reconstituted ShowBook should be marked as deleted")
                    .isTrue();
        }

    }

}