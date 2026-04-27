package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

import java.util.List;

public class ShowBookFactory {

    public static ShowBook reconstituteWithCreatedEvent() {
        ShowBookCreated customerCreated = new ShowBookCreated(ShowBookId.createRandom(), 1L, "customer name");
        return ShowBook.reconstitute(List.of(customerCreated));
    }

    public static ShowBook createDummy() {
        return ShowBook.create("show book name");
    }

}
