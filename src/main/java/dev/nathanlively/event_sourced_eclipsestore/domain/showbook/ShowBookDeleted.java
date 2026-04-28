package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

public record ShowBookDeleted(ShowBookId showBookId, Long eventSequence)
        implements ShowBookEvent {

    public ShowBookDeleted(ShowBookId showBookId) {
        this(showBookId, null);
    }

    @Override
    public ShowBookDeleted withSequence(Long eventSequence) {
        return new ShowBookDeleted(showBookId, eventSequence);
    }
}
