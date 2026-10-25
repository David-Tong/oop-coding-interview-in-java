package booking.validator;

import booking.Booking;

public class BookingValidator {
	public void validate(Booking booking) {
		// check id
		if (booking.getId() == null || booking.getId().length() == 0 || booking.getId().trim().length() == 0) {
			throw new IllegalArgumentException("Meeting ID can't be empty.");
		}
		// check time
		if (booking.getStartTime() >= booking.getEndTime()) {
			throw new IllegalArgumentException("Meeting start time can't be equal to or later than end time.");
		}
	}

}
