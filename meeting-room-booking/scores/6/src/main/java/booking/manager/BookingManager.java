package booking.manager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import booking.Booking;

public class BookingManager {

	Map<String, Booking> dict = new HashMap<>();
	TreeSet<Booking> bookings = new TreeSet<>(
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
		if (next != null && !next.getId().equals(booking.getId()) && next.getStartTime() < booking.getEndTime()) {
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

	public int findEarliestSlot(int from, int to, int duration) {
		if (from + duration > to) {
			return -1;
		}

		// check previous
		Booking current = this.bookings.floor(new Booking("", from, Integer.MAX_VALUE));
		int earliestStartTime;
		if (current == null) {
			earliestStartTime = from;
		} else {
			earliestStartTime = Math.max(from, current.getEndTime());
		}

		if (current != null) {
			current = this.bookings.higher(current);
		}
		while (current != null && earliestStartTime + duration <= to) {
			if (earliestStartTime + duration <= current.getStartTime()) {
				return earliestStartTime;
			}
			;
			earliestStartTime = current.getEndTime();
			current = bookings.higher(current);
		}

		if (earliestStartTime + duration <= to) {
			return earliestStartTime;
		} else {
			return -1;
		}
	}

}
