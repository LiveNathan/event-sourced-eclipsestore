package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

import java.util.List;

public class ShowBookFactory {

    public static ShowBook reconstituteWithCreatedEvent() {
        ShowBookCreated showBookCreated = new ShowBookCreated(ShowBookId.createRandom(), 1L, "show book name");
        return ShowBook.reconstitute(List.of(showBookCreated));
    }

    public static ShowBook createDummy() {
        return ShowBook.create(ShowBookId.createRandom(), "show book name");
    }

}
