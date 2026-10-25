package booking.reviewer;

import booking.Booking;
import booking.Query;
import booking.service.BookingService;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TestQueryBookingReview {
    private BookingService seeded() {
        BookingService s = new BookingService();
        s.reserver(new Booking("A", 60, 90));
        s.reserver(new Booking("B", 120, 150));
        return s;
    }
    // Compare values explicitly: Booking is not required to implement equals.
    private List<String> snapshot(BookingService s) {
        return s.listBookings().stream()
            .map(b -> b.getId() + ":" + b.getStartTime() + ":" + b.getEndTime()).toList();
    }
    @Test void searchBeforeFirstBooking() {
        BookingService s = seeded();
        assertEquals(150, s.findEarliestSlot(new Query(20, 200, 50)));
        assertEquals(-1, s.findEarliestSlot(new Query(20, 150, 50)));
    }
    @Test void failedRescheduleKeepsOriginal() {
        BookingService s = seeded();
        List<String> before = snapshot(s);
        assertThrows(RuntimeException.class, () -> s.reschedule(new Booking("A", 50, 130)));
        assertEquals(before, snapshot(s));
        assertTrue(s.cancel("A"));
        assertTrue(s.reserver(new Booking("C", 60, 90)));
    }
    @Test void rescheduleInBothDirectionsAndSameTime() {
        BookingService s = seeded();
        assertTrue(s.reschedule(new Booking("A", 70, 100)));
        assertTrue(s.reschedule(new Booking("A", 50, 80)));
        assertTrue(s.reschedule(new Booking("A", 50, 80)));
        assertEquals(List.of("A:50:80", "B:120:150"), snapshot(s));
    }
    @Test void invalidBookingAndReschedulePreserveState() {
        for (int[] t : new int[][] {{-1, 60}, {1400, 1441}, {90, 60}, {60, 60}}) {
            BookingService s = seeded();
            List<String> before = snapshot(s);
            assertThrows(RuntimeException.class, () -> s.reserver(new Booking("C", t[0], t[1])));
            assertThrows(RuntimeException.class, () -> s.reschedule(new Booking("A", t[0], t[1])));
            assertEquals(before, snapshot(s));
        }
    }
    @Test void validDurationLargerThanWindowIsNotAnError() {
        assertEquals(-1, new BookingService().findEarliestSlot(new Query(60, 90, 31)));
    }
    @Test void invalidQueryArguments() {
        assertAll(
            () -> assertThrows(RuntimeException.class, () -> seeded().findEarliestSlot(new Query(-1, 100, 10))),
            () -> assertThrows(RuntimeException.class, () -> seeded().findEarliestSlot(new Query(0, 1441, 10))),
            () -> assertThrows(RuntimeException.class, () -> seeded().findEarliestSlot(new Query(100, 100, 1))),
            () -> assertThrows(RuntimeException.class, () -> seeded().findEarliestSlot(new Query(100, 90, 1))),
            () -> assertThrows(RuntimeException.class, () -> seeded().findEarliestSlot(new Query(0, 100, 0))),
            () -> assertThrows(RuntimeException.class, () -> seeded().findEarliestSlot(new Query(0, 100, -1))),
            () -> assertThrows(RuntimeException.class, () -> seeded().findEarliestSlot(new Query(0, 1440, 1441)))
        );
    }
    @Test void exactGapAndFullDay() {
        assertEquals(90, seeded().findEarliestSlot(new Query(70, 180, 30)));
        assertEquals(-1, seeded().findEarliestSlot(new Query(70, 180, 31)));
        assertEquals(0, new BookingService().findEarliestSlot(new Query(0, 1440, 1440)));
    }
    @Test void queryAndListAreReadOnlyAndInstancesIndependent() {
        BookingService s = seeded();
        List<String> before = snapshot(s);
        s.findEarliestSlot(new Query(70, 180, 30));
        s.listBookings().clear();
        assertEquals(before, snapshot(s));
        assertTrue(new BookingService().listBookings().isEmpty());
    }
    @Test void bookingHasNoPublicSetters() {
        // API-level evidence only; does not assert that a non-final class cannot be subclassed.
        assertTrue(java.util.Arrays.stream(Booking.class.getMethods())
            .noneMatch(m -> m.getName().startsWith("set") && m.getParameterCount() == 1));
    }
    @Test void cancellationAndDuplicateIds() {
        BookingService s = seeded();
        assertThrows(RuntimeException.class, () -> s.reserver(new Booking(new String("A"), 200, 230)));
        assertThrows(RuntimeException.class, () -> s.cancel("missing"));
        assertTrue(s.cancel("A"));
        assertTrue(s.reserver(new Booking("A", 60, 90)));
    }
    @Test void conflictsFollowExceptionContract() {
        assertAll(
            () -> assertThrows(RuntimeException.class, () -> seeded().reserver(new Booking("C", 70, 80))),
            () -> assertThrows(RuntimeException.class, () -> seeded().reschedule(new Booking("A", 130, 160)))
        );
    }
    @Test void nullObjectsHaveExplicitValidation() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> seeded().reserver(null)),
            () -> assertThrows(IllegalArgumentException.class, () -> seeded().reschedule(null)),
            () -> assertThrows(IllegalArgumentException.class, () -> seeded().findEarliestSlot(null))
        );
    }
}
