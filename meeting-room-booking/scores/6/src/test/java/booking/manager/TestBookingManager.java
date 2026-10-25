package booking.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import booking.Booking;

class TestBookingManager {
    private BookingManager manager;

    @BeforeEach
    void setUp() {
        manager = new BookingManager();
    }

    @Test
    void reservesAndListsBookingsInTimeOrder() {
        Booking later = new Booking("B", 120, 150);
        Booking earlier = new Booking("A", 60, 90);
        assertTrue(manager.reserve(later));
        assertTrue(manager.reserve(earlier));
        assertEquals(List.of(earlier, later), manager.listBookings());
    }

    @Test
    void rejectsOverlapsWithoutChangingSchedule() {
        Booking existing = new Booking("A", 60, 90);
        manager.reserve(existing);
        for (Booking conflict : List.of(new Booking("B", 50, 70),
                new Booking("C", 70, 80), new Booking("D", 80, 100),
                new Booking("E", 50, 100), new Booking("F", 60, 90))) {
            assertFalse(manager.reserve(conflict), conflict.toString());
        }
        assertEquals(List.of(existing), manager.listBookings());
    }

    @Test
    void allowsAdjacentBookingsOnEitherSide() {
        manager.reserve(new Booking("A", 60, 90));
        assertTrue(manager.reserve(new Booking("B", 90, 120)));
        assertTrue(manager.reserve(new Booking("C", 30, 60)));
    }

    @Test
    void rejectsDuplicateIdsAtDifferentTimes() {
        manager.reserve(new Booking("A", 60, 90));
        assertThrows(IllegalArgumentException.class,
                () -> manager.reserve(new Booking(new String("A"), 120, 150)));
        assertEquals(1, manager.listBookings().size());
    }

    @Test
    void cancellationReleasesTime() {
        manager.reserve(new Booking("A", 60, 90));
        assertTrue(manager.cancel("A"));
        assertTrue(manager.listBookings().isEmpty());
        assertTrue(manager.reserve(new Booking("B", 60, 90)));
    }

    @Test
    void rejectsInvalidCancellationIds() {
        assertThrows(IllegalArgumentException.class, () -> manager.cancel(null));
        assertThrows(IllegalArgumentException.class, () -> manager.cancel(""));
        assertThrows(IllegalArgumentException.class, () -> manager.cancel("missing"));
    }

    @Test
    void reschedulesBookingOverItsOriginalTime() {
        manager.reserve(new Booking("A", 60, 90));
        Booking replacement = new Booking(new String("A"), 70, 100);
        assertTrue(manager.reschedule(replacement));
        assertEquals(List.of(replacement), manager.listBookings());
    }

    @Test
    void conflictingReschedulePreservesSchedule() {
        Booking original = new Booking("A", 60, 90);
        Booking other = new Booking("B", 120, 150);
        manager.reserve(original);
        manager.reserve(other);
        assertFalse(manager.reschedule(new Booking("A", 130, 160)));
        assertEquals(List.of(original, other), manager.listBookings());
    }

    @Test
    void rejectsReschedulingUnknownId() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.reschedule(new Booking("missing", 60, 90)));
    }

    @Test
    void listingReturnsAnIndependentList() {
        Booking existing = new Booking("A", 60, 90);
        manager.reserve(existing);
        manager.listBookings().clear();
        assertEquals(List.of(existing), manager.listBookings());
    }

    @Test
    void findsSlotInEmptySchedule() {
        assertEquals(60, manager.findEarliestSlot(60, 120, 30));
    }

    @Test
    void findsSlotBeforeFirstBooking() {
        manager.reserve(new Booking("A", 60, 90));
        assertEquals(20, manager.findEarliestSlot(20, 90, 40));
    }

    @Test
    void findsExactGapAndSkipsShortGaps() {
        manager.reserve(new Booking("A", 60, 90));
        manager.reserve(new Booking("B", 120, 150));
        assertEquals(90, manager.findEarliestSlot(70, 180, 30));
        assertEquals(-1, manager.findEarliestSlot(70, 180, 31));
        assertEquals(150, manager.findEarliestSlot(70, 200, 35));
    }

    @Test
    void slotStartsWithinRequestedWindow() {
        manager.reserve(new Booking("A", 60, 90));
        assertEquals(120, manager.findEarliestSlot(120, 180, 30));
    }

    @Test
    void returnsNoSlotForShortOrFullyBookedWindow() {
        assertEquals(-1, manager.findEarliestSlot(180, 200, 35));
        manager.reserve(new Booking("A", 60, 120));
        assertEquals(-1, manager.findEarliestSlot(60, 120, 30));
    }
}
