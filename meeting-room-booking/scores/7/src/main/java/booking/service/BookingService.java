package booking.service;

import java.util.List;

import booking.Booking;
import booking.Query;
import booking.manager.BookingManager;
import booking.validator.BookingValidator;

public class BookingService {
	
	private BookingValidator validator = new BookingValidator();
	private BookingManager manager = new BookingManager();
	
	public boolean reserver(Booking booking) {
		validator.validateBooking(booking);
		return manager.reserve(booking);
	}
	
	public boolean cancel(String id) {
		return manager.cancel(id);
	}
	
	public boolean reschedule(Booking newBooking) {
		validator.validateBooking(newBooking);
		return manager.reschedule(newBooking);
	}
	
	public List<Booking> listBookings() {
		return manager.listBookings();
	}
	
	public int findEarliestSlot(Query query) {
		validator.validateQuery(query);
		return manager.findEarliestSlot(query);
	}

}
