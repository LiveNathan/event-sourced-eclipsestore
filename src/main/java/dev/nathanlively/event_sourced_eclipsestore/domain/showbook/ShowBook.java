package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

import dev.nathanlively.event_sourced_eclipsestore.domain.EventSourcedAggregate;

import java.util.List;
import java.util.StringJoiner;

// Inspired by https://github.com/jitterted/jitterticket-event-sourced
public class ShowBook extends EventSourcedAggregate<ShowBookEvent, ShowBookId> {

    private String name;

    //region Creation Command
    public static ShowBook create(String name) {
        return new ShowBook(ShowBookId.createRandom(), name);
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
        enqueue(new ShowBookCreated(showBookId, null, name));
    }

    @Override
    protected void apply(ShowBookEvent showBookEvent) {
        switch (showBookEvent) {
            case ShowBookCreated registered -> {
                setId(registered.showBookId());
                this.name = registered.showBookName();
            }
            case ShowBookNameUpdated purchased -> {
            }
        }
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ShowBook.class.getSimpleName() + "[", "]")
                .add("id='" + getId() + "'")
                .add("name='" + name + "'")
                .toString();
    }

}
