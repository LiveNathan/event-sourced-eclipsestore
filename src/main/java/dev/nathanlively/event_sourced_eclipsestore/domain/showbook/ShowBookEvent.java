package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

import dev.nathanlively.event_sourced_eclipsestore.domain.Event;

public abstract sealed class ShowBookEvent extends Event permits ShowBookCreated, ShowBookNameUpdated {

    private final ShowBookId showBookId;

    protected ShowBookEvent(ShowBookId showBookId, Long eventSequence) {
        super(eventSequence);
        this.showBookId = showBookId;
    }

    public ShowBookId showBookId() {
        return showBookId;
    }
}
