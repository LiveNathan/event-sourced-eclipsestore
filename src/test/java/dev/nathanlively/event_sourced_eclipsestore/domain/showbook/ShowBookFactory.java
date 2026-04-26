package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

import java.util.List;

public class ShowBookFactory {

    public static ShowBook reconstituteWithRegisteredEvent() {
        ShowBookCreated customerRegistered = new ShowBookCreated(ShowBookId.createRandom(), 1L, "customer name");
        return ShowBook.reconstitute(List.of(customerRegistered));
    }

    public static ShowBook newlyRegistered() {
        return ShowBook.create(ShowBookId.createRandom(), "customer name");
    }

}
