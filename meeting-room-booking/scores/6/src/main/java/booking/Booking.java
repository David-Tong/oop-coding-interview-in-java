package booking;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Booking {
	private String id;
	private int startTime;
	private int endTime;
}
