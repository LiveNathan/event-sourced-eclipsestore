package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

public record ShowBookNameUpdated(ShowBookId showBookId, Long eventSequence, String name)
        implements ShowBookEvent {

    public ShowBookNameUpdated(ShowBookId showBookId, String name) {
        this(showBookId, null, name);
    }

    @Override
    public ShowBookNameUpdated withSequence(Long eventSequence) {
        return new ShowBookNameUpdated(showBookId, eventSequence, name);
    }
}
