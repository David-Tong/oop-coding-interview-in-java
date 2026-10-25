package booking.manager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import booking.Booking;
import booking.Query;

public class BookingManager {

	private Map<String, Booking> dict = new HashMap<>();
	private TreeSet<Booking> bookings = new TreeSet<>(
			Comparator.comparingInt(Booking::getStartTime).thenComparingInt(Booking::getEndTime));

	boolean canReserve(Booking booking) {
		int startTime = booking.getStartTime();
		// check previous
		Booking prev = this.bookings.floor(new Booking("", startTime, startTime));
		if (prev != null && !prev.getId().equals(booking.getId()) && prev.getEndTime() > startTime) {
			return false;
		}

		// check next
		Booking next = this.bookings.ceiling(new Booking("", startTime, startTime));
		// skip yourself
		if (next != null && next.getId().equals(booking.getId())) {
			next = this.bookings.higher(next);
		}
		if (next != null && next.getStartTime() < booking.getEndTime()) {
			return false;
		}
		return true;
	}

	public boolean reserve(Booking booking) {
		if (this.dict.containsKey(booking.getId())) {
			throw new IllegalArgumentException(
					"Can't reserve a meeting with a reserved ID %s ".formatted(booking.getId()));
		}

		if (this.canReserve(booking)) {
			try {
				this.dict.put(booking.getId(), booking);
				this.bookings.add(booking);
			} catch (Exception ex) {
				throw ex;
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean cancel(String id) {
		if (id == null || id.length() == 0) {
			throw new IllegalArgumentException("ID can't be empty");
		}

		if (this.dict.containsKey(id)) {
			try {
				Booking booking = this.dict.get(id);
				this.dict.remove(booking.getId());
				this.bookings.remove(booking);
			} catch (Exception ex) {
				throw new IllegalStateException("Booking manager status abnormal.");
			}
		} else {
			throw new IllegalArgumentException("Meeting with ID %s can't be found".formatted(id));
		}
		return true;
	}

	public boolean reschedule(Booking newBooking) {
		String id = newBooking.getId();
		if (!this.dict.containsKey(id)) {
			throw new IllegalArgumentException("Can't reschedule a meeting not reserverd with ID %s.".formatted(id));
		}

		if (this.canReserve(newBooking)) {
			if (this.cancel(id)) {
				if (this.reserve(newBooking)) {
					return true;
				} else {
					throw new IllegalStateException(
							"We cancel the meeting with ID %s but unable to reserve it with new time.".formatted(id));
				}
			}
		}
		return false;
	}

	public List<Booking> listBookings() {
		return new ArrayList<>(this.bookings);
	}

	public int findEarliestSlot(Query query) {
		// check previous
		Booking prev = this.bookings.floor(new Booking("", query.getFrom(), Integer.MAX_VALUE));
		int earliestStartTime;
		if (prev == null) {
			earliestStartTime = query.getFrom();
		} else {
			earliestStartTime = Math.max(query.getFrom(), prev.getEndTime());
		}

		// check next
		Booking next = this.bookings.ceiling(new Booking("", query.getFrom(), query.getFrom()));
		while (next != null && earliestStartTime + query.getDuration() <= query.getTo()) {
			if (earliestStartTime + query.getDuration() <= next.getStartTime()) {
				return earliestStartTime;
			}
			earliestStartTime = next.getEndTime();
			next = bookings.higher(next);
		}
		
		if (earliestStartTime + query.getDuration() <= query.getTo()) {
			return earliestStartTime;
		}
		
		return -1;
	}

}
