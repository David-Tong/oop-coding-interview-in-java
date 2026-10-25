package booking.service;

import java.util.List;

import booking.Booking;
import booking.manager.BookingManager;
import booking.validator.BookingValidator;

public class BookingService {
	
	private BookingValidator validator = new BookingValidator();
	private BookingManager manager = new BookingManager();
	
	public boolean reserver(Booking booking) {
		validator.validate(booking);
		return manager.reserve(booking);
	}
	
	public boolean cancel(String id) {
		return manager.cancel(id);
	}
	
	public boolean reschedule(Booking newBooking) {
		return manager.reschedule(newBooking);
	}
	
	public List<Booking> listBookings() {
		return manager.listBookings();
	}
	
	public int findEarliestSlot(int from, int to, int duration) {
		return manager.findEarliestSlot(from, to, duration);
	}

}
