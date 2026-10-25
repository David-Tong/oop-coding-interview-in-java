package booking.validator;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import booking.Booking;

class TestBookingValidator {
    private final BookingValidator validator = new BookingValidator();

    @Test
    void testValidate() {
        assertDoesNotThrow(() -> validator.validate(new Booking("A", 60, 90)));
        assertDoesNotThrow(() -> validator.validate(new Booking("123456789", 60, 90)));
        assertDoesNotThrow(() -> validator.validate(new Booking(" meeting ", 60, 90)));

        assertThrows(IllegalArgumentException.class, () -> validator.validate(new Booking(null, 60, 90)));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new Booking("", 60, 90)));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new Booking(" ", 60, 90)));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new Booking("   ", 60, 90)));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new Booking("\t", 60, 90)));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new Booking("\n", 60, 90)));

        assertThrows(IllegalArgumentException.class, () -> validator.validate(new Booking("A", 60, 60)));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new Booking("A", 90, 60)));
    }
}
