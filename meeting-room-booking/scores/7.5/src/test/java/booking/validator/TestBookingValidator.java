package booking.validator;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import booking.Booking;
import booking.Query;

class TestBookingValidator {
    private final BookingValidator validator = new BookingValidator();

    @ParameterizedTest
    @ValueSource(strings = {"A", "123456789", " meeting "})
    void validateBookingAcceptsNonEmptyId(String id) {
        assertDoesNotThrow(() -> validator.validateBooking(new Booking(id, 60, 90)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n", "\r\n", " \t\n "})
    void validateBookingRejectsEmptyId(String id) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validator.validateBooking(new Booking(id, 60, 90)));

        assertEquals("Meeting ID can't be empty.", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "60, 90", "0, 1439", "1438, 1439"})
    void validateQueryAcceptsValidTimeRange(int from, int to) {
        assertDoesNotThrow(() -> validator.validateQuery(new Query(from, to, 1)));
    }

    @ParameterizedTest
    @CsvSource({"-1, 60", "0, 0", "60, 60", "90, 60", "60, 1440", "60, 1441"})
    void validateQueryRejectsInvalidTimeRange(int from, int to) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validator.validateQuery(new Query(from, to, 1)));

        assertEquals("Illegal query condition.", exception.getMessage());
    }

    @Test
    void validateBookingRejectsInvalidTimeRange() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateBooking(new Booking("A", -60, 90)));
        assertThrows(IllegalArgumentException.class, () -> validator.validateBooking(new Booking("A", 60, 20)));

        assertThrows(IllegalArgumentException.class, () -> validator.validateBooking(new Booking("A", 60, 60)));
        assertThrows(IllegalArgumentException.class, () -> validator.validateBooking(new Booking("A", 90, 60)));
    }
}
