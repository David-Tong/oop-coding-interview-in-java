package booking.reviewer;

import booking.Query;
import booking.service.BookingService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TestQueryBoundariesReview {
    @Test void dayEndIsInclusiveQueryBoundary() {
        assertEquals(0, new BookingService().findEarliestSlot(new Query(0, 1440, 1440)));
    }
    @Test void zeroDurationIsRejected() {
        assertThrows(RuntimeException.class,
            () -> new BookingService().findEarliestSlot(new Query(0, 100, 0)));
    }
    @Test void negativeDurationIsRejected() {
        assertThrows(RuntimeException.class,
            () -> new BookingService().findEarliestSlot(new Query(0, 100, -1)));
    }
    @Test void durationBeyondDayIsRejected() {
        assertThrows(RuntimeException.class,
            () -> new BookingService().findEarliestSlot(new Query(0, 100, 1441)));
    }
}
