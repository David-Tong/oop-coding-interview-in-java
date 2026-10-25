package booking;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Query {
	private int from;
	private int to;
	private int duration;
}
