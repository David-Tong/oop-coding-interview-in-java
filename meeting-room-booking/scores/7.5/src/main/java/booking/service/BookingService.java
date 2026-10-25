package booking.service;

import java.util.List;

import booking.Booking;
import booking.Query;
import booking.validator.BookingValidator;

public final class BookingService {
	
	private final  BookingValidator validator = new BookingValidator();
	private final  BookingManager manager = new BookingManager();
	
	public boolean reserver(Booking booking) {
		validator.validateBooking(booking);
		return manager.reserve(booking);
	}
	
	public boolean cancel(String id) {
		return manager.cancel(id);
	}
	
	public boolean reschedule(Booking replacement) {
		validator.validateBooking(replacement);
		return manager.reschedule(replacement);
	}
	
	public List<Booking> listBookings() {
		return manager.listBookings();
	}
	
	public int findEarliestSlot(Query query) {
		validator.validateQuery(query);
		return manager.findEarliestSlot(query);
	}

}
