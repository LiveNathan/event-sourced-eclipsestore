package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

import dev.nathanlively.event_sourced_eclipsestore.domain.EventSourcedAggregate;

import java.util.List;
import java.util.StringJoiner;

// Inspired by https://github.com/jitterted/jitterticket-event-sourced
public class ShowBook extends EventSourcedAggregate<ShowBookEvent, ShowBookId> {

    private String name;
    private boolean deleted;

    //region Creation Command
    public static ShowBook create(ShowBookId showBookId, String name) {
        validateName(name);
        return new ShowBook(showBookId, name);
    }

    private static void validateName(String name) {
        if (name.isBlank()) throw new IllegalArgumentException("Name must not be null or blank.");
    }
    //endregion

    //region Commands
    public void rename(String newName) {
        if (deleted) {
            throw new IllegalStateException("Cannot rename a deleted ShowBook.");
        }
        validateName(newName);
        enqueue(new ShowBookNameUpdated(id(), newName));
    }

    public void delete() {
        if (deleted) {
            throw new IllegalStateException("ShowBook is already deleted.");
        }
        enqueue(new ShowBookDeleted(id()));
    }
    //endregion

    //region Event Application
    public static ShowBook reconstitute(List<ShowBookEvent> showBookEvents) {
        return new ShowBook(showBookEvents);
    }

    private ShowBook(List<ShowBookEvent> showBookEvents) {
        applyAll(showBookEvents);
    }

    private ShowBook(ShowBookId showBookId, String name) {
        enqueue(new ShowBookCreated(showBookId, name));
    }

    @Override
    protected void apply(ShowBookEvent showBookEvent) {
        switch (showBookEvent) {
            case ShowBookCreated registered -> {
                setId(registered.showBookId());
                this.name = registered.showBookName();
            }
            case ShowBookNameUpdated renamed -> this.name = renamed.name();
            case ShowBookDeleted ignored -> this.deleted = true;
        }
    }
    //endregion

    //region Queries
    public String name() {
        return name;
    }

    public boolean isDeleted() {
        return deleted;
    }
    //endregion Queries

    @Override
    public String toString() {
        return new StringJoiner(", ", ShowBook.class.getSimpleName() + "[", "]")
                .add("id='" + id() + "'")
                .add("name='" + name + "'")
                .toString();
    }

}
