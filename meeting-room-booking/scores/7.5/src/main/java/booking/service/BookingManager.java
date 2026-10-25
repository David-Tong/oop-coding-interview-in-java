package booking.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import booking.Booking;
import booking.Query;

final class BookingManager {

	private Map<String, Booking> dict = new HashMap<>();
	private TreeSet<Booking> bookings = new TreeSet<>(
			Comparator.comparingInt(Booking::getStartTime).thenComparingInt(Booking::getEndTime));

	boolean canReserve(Booking booking) {
		int startTime = booking.getStartTime();
		// check previous
		Booking prev = bookings.floor(new Booking("", startTime, startTime));
		if (prev != null && !prev.getId().equals(booking.getId()) && prev.getEndTime() > startTime) {
			return false;
		}

		// check next
		Booking next = bookings.ceiling(new Booking("", startTime, startTime));
		// skip yourself
		if (next != null && next.getId().equals(booking.getId())) {
			next = bookings.higher(next);
		}
		if (next != null && next.getStartTime() < booking.getEndTime()) {
			return false;
		}
		return true;
	}

	boolean reserve(Booking booking) {
		if (dict.containsKey(booking.getId())) {
			throw new IllegalArgumentException(
					"Can't reserve a meeting with a reserved ID %s ".formatted(booking.getId()));
		}

		if (canReserve(booking)) {
			try {
				dict.put(booking.getId(), booking);
				bookings.add(booking);
			} catch (Exception ex) {
				throw new IllegalStateException("Booking manager status abnormal.");
			}
			return true;
		} else {
			throw new IllegalArgumentException(
					"Can't reserve a meeting betwen %d and %d ".formatted(booking.getStartTime(), booking.getEndTime()));
		}
	}

	boolean cancel(String id) {
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("ID can't be empty");
		}
		if (dict.containsKey(id)) {
			try {
				Booking booking = dict.get(id);
				dict.remove(booking.getId());
				bookings.remove(booking);
			} catch (Exception ex) {
				throw new IllegalStateException("Booking manager status abnormal.");
			}
		} else {
			throw new IllegalArgumentException("Meeting with ID %s can't be found".formatted(id));
		}
		return true;
	}

	boolean reschedule(Booking replacement) {
		String id = replacement.getId();
		Booking original = dict.get(id);

		if (original == null) {
			throw new IllegalArgumentException("Booking not found: " + id);
		}

		if (!canReserve(replacement)) {
			throw new IllegalStateException("Cam reschedule the meeting : " + id);
		}

		bookings.remove(original);
		bookings.add(replacement);
		dict.put(id, replacement);
		return true;
	}

	List<Booking> listBookings() {
		return new ArrayList<>(bookings);
	}

	int findEarliestSlot(Query query) {
		if (query.getFrom() + query.getDuration() > query.getTo()) {
			return -1;
		}

		// check previous
		Booking prev = bookings.floor(new Booking("", query.getFrom(), Integer.MAX_VALUE));
		int earliestStartTime;
		if (prev == null) {
			earliestStartTime = query.getFrom();
		} else {
			earliestStartTime = Math.max(query.getFrom(), prev.getEndTime());
		}

		// check next
		Booking next = bookings.ceiling(new Booking("", query.getFrom(), query.getFrom()));
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
