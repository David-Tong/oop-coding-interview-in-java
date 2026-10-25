package booking.validator;

import booking.Booking;
import booking.Query;

public class BookingValidator {
	public void validateBooking(Booking booking) {
		if (booking == null) {
			throw new IllegalArgumentException("Booking must not be null.");
		}

		// check id
		if (booking.getId() == null || booking.getId().isEmpty() || booking.getId().trim().isEmpty()) {
			throw new IllegalArgumentException("Meeting ID can't be empty.");
		}
		// check time
		if (booking.getStartTime() >= booking.getEndTime() || booking.getStartTime() < 0 || booking.getEndTime() > 1440) {
			throw new IllegalArgumentException("Meeting start time can't be equal to or later than end time.");
		}
	}

	public void validateQuery(Query query) {
		if (query == null) {
			throw new IllegalArgumentException("Query must not be null.");
		}

		if (0 <= query.getFrom() && query.getFrom() < query.getTo() && query.getTo() < 1440) {
		} else {
			throw new IllegalArgumentException("Illegal query condition.");
		}
	}
}
