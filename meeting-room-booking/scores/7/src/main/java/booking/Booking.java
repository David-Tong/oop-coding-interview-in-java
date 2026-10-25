package booking;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Booking {
	private String id;
	private int startTime;
	private int endTime;
}
