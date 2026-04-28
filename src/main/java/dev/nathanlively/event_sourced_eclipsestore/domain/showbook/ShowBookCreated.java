package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

public record ShowBookCreated(ShowBookId showBookId, Long eventSequence, String name) implements ShowBookEvent {

    public ShowBookCreated(ShowBookId showBookId, String name) {
        this(showBookId, null, name);
    }

    @Override
    public ShowBookCreated withSequence(Long eventSequence) {
        return new ShowBookCreated(showBookId, eventSequence, name);
    }
}
