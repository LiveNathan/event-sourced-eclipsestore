package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

import java.util.Objects;
import java.util.StringJoiner;

public final class ShowBookCreated extends ShowBookEvent {
    private final String name;

    ShowBookCreated(ShowBookId showBookId, Long eventSequence, String name) {
        super(showBookId, eventSequence);
        this.name = name;
    }

    public ShowBookCreated(ShowBookId showBookId, String name) {
        this(showBookId, null, name);
    }

    public String showBookName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ShowBookCreated that = (ShowBookCreated) o;
        return Objects.equals(showBookId(), that.showBookId()) &&
               Objects.equals(eventSequence(), that.eventSequence()) &&
               Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(showBookId(), eventSequence(), name);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ShowBookCreated.class.getSimpleName() + "[", "]")
                .add("customerId='" + showBookId() + "'")
                .add("eventSequence=" + eventSequence())
                .add("customerName='" + name + "'")
                .toString();
    }
}
