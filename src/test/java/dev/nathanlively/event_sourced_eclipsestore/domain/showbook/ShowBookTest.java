package dev.nathanlively.event_sourced_eclipsestore.domain.showBook;

import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBook;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookCreated;
import dev.nathanlively.event_sourced_eclipsestore.domain.showbook.ShowBookId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Inspired by https://github.com/jitterted/jitterticket-event-sourced
class ShowBookTest {

    @Nested
    class CommandsGenerateEvents {
        @Test
        void createShowBookGeneratesShowBookCreated() {
            ShowBookId showBookId = ShowBookId.createRandom();

            ShowBook showBook = ShowBook.create(showBookId, "show book name");

            assertThat(showBook.uncommittedEvents())
                    .containsExactly(
                            new ShowBookCreated(showBookId, null, "show book name")
                    );
        }
    }

    @Nested
    class EventsProjectState {

        @Test
        void showBookCreatedUpdatesName() {
            ShowBookId showBookId = ShowBookId.createRandom();
            ShowBookCreated showBookCreated = new ShowBookCreated(showBookId, 0L, "showBook name");

            ShowBook showBook = ShowBook.reconstitute(List.of(showBookCreated));

            assertThat(showBook.getId())
                    .isEqualTo(showBookId);
            assertThat(showBook.name())
                    .isEqualTo("showBook name");
        }

//        @Test
//        void ticketsPurchasedAddsTicketOrder() {
//            ShowBookId showBookId = ShowBookId.createRandom();
//            ShowBookCreated showBookCreated = new ShowBookCreated(
//                    showBookId, 1L, "showBook name");
//            ConcertId concertId = ConcertId.createRandom();
//            int quantity = 8;
//            int amountPaid = quantity * 45;
//            TicketOrderId ticketOrderId = TicketOrderId.createRandom();
//            TicketsPurchased ticketsPurchased = new TicketsPurchased(
//                    showBookId, 2L, ticketOrderId, concertId, quantity, amountPaid);
//
//            ShowBook showBook = ShowBook.reconstitute(List.of(
//                    showBookCreated, ticketsPurchased));
//
//            ShowBook.TicketOrder expectedTicketOrder = new ShowBook.TicketOrder(
//                    ticketOrderId, concertId, quantity, amountPaid);
//            assertThat(showBook.ticketOrders())
//                    .containsExactly(expectedTicketOrder);
//            assertThat(showBook.ticketOrderFor(ticketOrderId))
//                    .as("Expected ticketOrderFor() to find the ticket order by its ID")
//                    .isPresent()
//                    .get()
//                    .isEqualTo(expectedTicketOrder);
//        }
//
//        @Test
//        void ticketOrderForUnknownTicketIdIsEmptyOptional() {
//            ShowBookId showBookId = ShowBookId.createRandom();
//            ShowBookCreated showBookCreated = new ShowBookCreated(
//                    showBookId, 1L, "showBook name");
//            ShowBook showBook = ShowBook.reconstitute(List.of(showBookCreated));
//
//            Optional<ShowBook.TicketOrder> ticketOrder = showBook.ticketOrderFor(TicketOrderId.createRandom());
//
//            assertThat(ticketOrder)
//                    .as("Expected no Ticket Order for the unknown Ticket Order ID")
//                    .isEmpty();
//        }
    }

}