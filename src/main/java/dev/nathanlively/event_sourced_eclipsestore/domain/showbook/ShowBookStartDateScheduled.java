package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

import java.time.ZonedDateTime;

public record ShowBookStartDateScheduled(ShowBookId showBookId, Long eventSequence, ZonedDateTime startDate)
        implements ShowBookEvent {

    public ShowBookStartDateScheduled(ShowBookId showBookId, ZonedDateTime startDate) {
        this(showBookId, null, startDate);
    }

    @Override
    public ShowBookStartDateScheduled withSequence(Long eventSequence) {
        return new ShowBookStartDateScheduled(showBookId, eventSequence, startDate);
    }
}
