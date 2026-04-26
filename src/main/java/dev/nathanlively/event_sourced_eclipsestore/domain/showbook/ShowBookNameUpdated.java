package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

import java.util.Objects;
import java.util.StringJoiner;

public final class ShowBookNameUpdated extends ShowBookEvent {
    private final String name;

    public ShowBookNameUpdated(ShowBookId showBookId, Long eventSequence, String name) {
        super(showBookId, eventSequence);
        this.name = name;
    }

    public ShowBookNameUpdated(ShowBookId showBookId, String name) {
        this(showBookId, null, name);
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShowBookNameUpdated that = (ShowBookNameUpdated) o;
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
        return new StringJoiner(", ", ShowBookNameUpdated.class.getSimpleName() + "[", "]")
                .add("customerId='" + showBookId() + "'")
                .add("eventSequence=" + eventSequence())
                .add("ticketOrderId=" + name)
                .toString();
    }
}
