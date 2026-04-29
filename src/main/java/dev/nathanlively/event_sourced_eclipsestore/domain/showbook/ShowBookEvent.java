package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

import dev.nathanlively.event_sourced_eclipsestore.domain.Event;

public sealed interface ShowBookEvent extends Event permits ShowBookCreated, ShowBookNameUpdated, ShowBookDeleted, ShowBookStartDateScheduled {

    ShowBookId showBookId();

    ShowBookEvent withSequence(Long eventSequence);
}
