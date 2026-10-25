package booking.validator;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import booking.Booking;

class TestBookingValidator {
    private final BookingValidator validator = new BookingValidator();

    @Test
    void testValidate() {
        assertDoesNotThrow(() -> validator.validateBooking(new Booking("A", 60, 90)));
        assertDoesNotThrow(() -> validator.validateBooking(new Booking("123456789", 60, 90)));
        assertDoesNotThrow(() -> validator.validateBooking(new Booking(" meeting ", 60, 90)));
        
        assertThrows(IllegalArgumentException.class, () -> validator.validateBooking(new Booking("A", -60, 90)));
        assertThrows(IllegalArgumentException.class, () -> validator.validateBooking(new Booking("A", 60, 20)));
        assertThrows(IllegalArgumentException.class, () -> validator.validateBooking(new Booking(null, 60, 90)));
        assertThrows(IllegalArgumentException.class, () -> validator.validateBooking(new Booking("", 60, 90)));
        assertThrows(IllegalArgumentException.class, () -> validator.validateBooking(new Booking(" ", 60, 90)));
        assertThrows(IllegalArgumentException.class, () -> validator.validateBooking(new Booking("   ", 60, 90)));
        assertThrows(IllegalArgumentException.class, () -> validator.validateBooking(new Booking("\t", 60, 90)));
        assertThrows(IllegalArgumentException.class, () -> validator.validateBooking(new Booking("\n", 60, 90)));

        assertThrows(IllegalArgumentException.class, () -> validator.validateBooking(new Booking("A", 60, 60)));
        assertThrows(IllegalArgumentException.class, () -> validator.validateBooking(new Booking("A", 90, 60)));
    }
}
