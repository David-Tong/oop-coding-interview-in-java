package booking.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import booking.Booking;
import booking.Query;

class TestBookingService {
    private final BookingService service = new BookingService();

    @Test
    void reservesAndListsValidBooking() {
        Booking booking = new Booking("A", 60, 90);
        assertTrue(service.reserver(booking));
        assertEquals(List.of(booking), service.listBookings());
    }

    @Test
    void invalidReservationLeavesScheduleEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> service.reserver(new Booking("A", 90, 60)));
        assertTrue(service.listBookings().isEmpty());
    }

    @Test
    void reportsReservationConflict() {
        Booking existing = new Booking("A", 60, 90);
        service.reserver(existing);
        assertThrows(IllegalArgumentException.class,
                () -> service.reserver(new Booking("B", 70, 100)));
        assertEquals(List.of(existing), service.listBookings());
    }

    @Test
    void supportsBookingLifecycle() {
        service.reserver(new Booking("A", 60, 90));
        Booking replacement = new Booking("A", 120, 150);
        assertTrue(service.reschedule(replacement));
        assertEquals(List.of(replacement), service.listBookings());
        assertTrue(service.cancel("A"));
        assertTrue(service.listBookings().isEmpty());
    }

    @Test
    void findsTimeBetweenBookings() {
        service.reserver(new Booking("A", 60, 90));
        service.reserver(new Booking("B", 120, 150));
        assertEquals(90, service.findEarliestSlot(new Query(70, 180, 30)));
    }
}
