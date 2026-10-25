package booking.validator;

import booking.Booking;
import booking.Query;

public class BookingValidator {
	public void validateBooking(Booking booking) {
		// check id
		if (booking.getId() == null || booking.getId().length() == 0 || booking.getId().trim().length() == 0) {
			throw new IllegalArgumentException("Meeting ID can't be empty.");
		}
		// check time
		if (booking.getStartTime() >= booking.getEndTime() || booking.getStartTime() < 0 || booking.getEndTime() > 1440) {
			throw new IllegalArgumentException("Meeting start time can't be equal to or later than end time.");
		}
	}

	public void validateQuery(Query query) {
		if (query.getFrom() + query.getDuration() > query.getTo()) {
			throw new IllegalArgumentException("Illegal query condition.");
		}
	}
}
