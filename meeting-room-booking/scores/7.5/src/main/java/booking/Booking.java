package booking;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public final class Booking {
	final private String id;
	final private int startTime;
	final private int endTime;
}
