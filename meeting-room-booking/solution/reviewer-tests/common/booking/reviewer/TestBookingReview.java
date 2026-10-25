package booking.reviewer;

import booking.Booking;
import booking.service.BookingService;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Review tests use the existing public service API, including its reserver spelling. */
class TestBookingReview {
    private BookingService seeded() {
        BookingService service = new BookingService();
        service.reserver(new Booking("A", 60, 90));
        service.reserver(new Booking("B", 120, 150));
        return service;
    }

    // Conflict false is accepted here to test state independently of exception semantics.
    private void conflict(Runnable operation) {
        try { operation.run(); }
        catch (RuntimeException expected) {
            assertNotNull(expected.getMessage());
            assertFalse(expected.getMessage().isBlank());
        }
    }

    @Test void adjacencyAndSortedListing() {
        BookingService s = seeded();
        assertTrue(s.reserver(new Booking("C", 90, 120)));
        assertEquals(List.of("A", "C", "B"), s.listBookings().stream().map(Booking::getId).toList());
    }

    @Test void duplicateAndUnknownIdsDoNotChangeState() {
        BookingService s = seeded();
        List<Booking> before = s.listBookings();
        assertThrows(RuntimeException.class, () -> s.reserver(new Booking(new String("A"), 200, 230)));
        assertThrows(RuntimeException.class, () -> s.cancel("missing"));
        assertThrows(RuntimeException.class, () -> s.reschedule(new Booking("missing", 200, 230)));
        assertEquals(before, s.listBookings());
    }

    @Test void cancellationReleasesIdAndTime() {
        BookingService s = seeded();
        assertTrue(s.cancel(new String("A")));
        assertTrue(s.reserver(new Booking("A", 60, 90)));
    }

    @Test void idsKeepCaseAndWhitespace() {
        BookingService s = new BookingService();
        assertTrue(s.reserver(new Booking("A", 0, 10)));
        assertTrue(s.reserver(new Booking("a", 10, 20)));
        assertTrue(s.reserver(new Booking(" A ", 20, 30)));
        assertEquals(3, s.listBookings().size());
    }

    @Test void sameTimeRescheduleSucceeds() {
        BookingService s = seeded();
        assertTrue(s.reschedule(new Booking(new String("A"), 60, 90)));
        assertEquals(2, s.listBookings().size());
    }

    @Test void failedRescheduleBehindSelfKeepsOriginalAndRemainsUsable() {
        BookingService s = seeded();
        conflict(() -> s.reschedule(new Booking("A", 50, 130)));
        assertEquals(List.of(new Booking("A", 60, 90), new Booking("B", 120, 150)), s.listBookings());
        assertTrue(s.cancel("A"));
        assertTrue(s.reserver(new Booking("C", 60, 90)));
    }

    @Test void searchBeforeFirstBookingSkipsOccupiedTime() {
        BookingService s = seeded();
        assertEquals(150, s.findEarliestSlot(20, 200, 50));
    }

    @Test void searchBeforeFirstBookingCanReportNoSlot() {
        BookingService s = seeded();
        assertEquals(-1, s.findEarliestSlot(20, 150, 50));
    }

    @Test void exactGapAndNonContiguousGaps() {
        BookingService s = seeded();
        assertEquals(90, s.findEarliestSlot(70, 180, 30));
        assertEquals(-1, s.findEarliestSlot(70, 180, 31));
    }

    @Test void fullDayAndTooLongDuration() {
        BookingService s = new BookingService();
        assertEquals(0, s.findEarliestSlot(0, 1440, 1440));
        assertEquals(-1, s.findEarliestSlot(60, 90, 31));
    }

    @Test void queriesAndInstancesAreIndependent() {
        BookingService s = seeded();
        List<Booking> before = s.listBookings();
        assertEquals(90, s.findEarliestSlot(70, 180, 30));
        assertEquals(90, s.findEarliestSlot(70, 180, 30));
        assertEquals(before, s.listBookings());
        assertTrue(new BookingService().listBookings().isEmpty());
    }

    @Test void reservationRejectsOutsideDay() {
        for (Booking b : List.of(new Booking("A", -1, 60), new Booking("B", 1400, 1441))) {
            BookingService s = new BookingService();
            assertThrows(RuntimeException.class, () -> s.reserver(b), b.toString());
            assertTrue(s.listBookings().isEmpty());
        }
    }

    @Test void invalidReschedulePreservesOriginal() {
        for (Booking replacement : List.of(new Booking("A", 90, 60), new Booking("A", 60, 60),
                new Booking("A", -1, 30), new Booking("A", 1400, 1441))) {
            BookingService s = seeded();
            assertThrows(RuntimeException.class, () -> s.reschedule(replacement), replacement.toString());
            assertEquals(List.of(new Booking("A", 60, 90), new Booking("B", 120, 150)), s.listBookings());
        }
    }

    @Test void invalidSearchArgumentsAreErrors() {
        for (int[] args : new int[][] {{-1, 100, 10}, {0, 1441, 10}, {100, 100, 1},
                {100, 90, 1}, {0, 100, 0}, {0, 100, -1}, {0, 100, 1441}}) {
            BookingService s = seeded();
            assertThrows(RuntimeException.class, () -> s.findEarliestSlot(args[0], args[1], args[2]));
        }
    }

    @Test void reservationInputObjectCannotMutateStoredState() {
        BookingService s = new BookingService();
        Booking input = new Booking("A", 60, 90);
        s.reserver(input);
        input.setId("changed");
        input.setStartTime(200);
        input.setEndTime(230);
        assertEquals(List.of(new Booking("A", 60, 90)), s.listBookings());
        assertTrue(s.cancel("A"));
    }

    @Test void returnedRecordCannotMutateStoredState() {
        BookingService s = seeded();
        try { s.listBookings().get(0).setStartTime(200); }
        catch (UnsupportedOperationException allowed) { /* Immutable view is also valid. */ }
        assertEquals(List.of(new Booking("A", 60, 90), new Booking("B", 120, 150)), s.listBookings());
        assertTrue(s.cancel("A"));
    }

    @Test void returnedCollectionCannotMutateStoredState() {
        BookingService s = seeded();
        try { s.listBookings().clear(); }
        catch (UnsupportedOperationException allowed) { /* Immutable collection is also valid. */ }
        assertEquals(2, s.listBookings().size());
    }

    @Test void conflictsAreReportedAsErrorsPerProblemContract() {
        BookingService s = seeded();
        assertThrows(RuntimeException.class, () -> s.reserver(new Booking("C", 70, 80)));
    }

    @Test void rescheduleConflictsAreReportedAsErrorsPerProblemContract() {
        BookingService s = seeded();
        assertThrows(RuntimeException.class, () -> s.reschedule(new Booking("A", 130, 160)));
    }

    @Test void nullBookingHasExplicitReason() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> new BookingService().reserver(null));
        assertFalse(ex instanceof NullPointerException, "Reject null explicitly rather than dereferencing it");
        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().isBlank());
    }
}
