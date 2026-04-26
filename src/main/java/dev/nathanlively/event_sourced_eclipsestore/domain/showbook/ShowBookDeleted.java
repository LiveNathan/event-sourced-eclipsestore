package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

import java.util.Objects;
import java.util.StringJoiner;

public final class ShowBookDeleted extends ShowBookEvent {

    public ShowBookDeleted(ShowBookId showBookId, Long eventSequence) {
        super(showBookId, eventSequence);
    }

    public ShowBookDeleted(ShowBookId showBookId) {
        this(showBookId, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShowBookDeleted that = (ShowBookDeleted) o;
        return Objects.equals(showBookId(), that.showBookId()) &&
               Objects.equals(eventSequence(), that.eventSequence());
    }

    @Override
    public int hashCode() {
        return Objects.hash(showBookId(), eventSequence());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ShowBookDeleted.class.getSimpleName() + "[", "]")
                .add("showBookId='" + showBookId() + "'")
                .add("eventSequence=" + eventSequence())
                .toString();
    }
}
