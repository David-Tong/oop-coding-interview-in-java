package calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TestCalculator {
	
	@Test
	void testSuccessfulExecution() {
		String input = "13, 5, +, 9, -, U, +";
		Calculator calculator = new Calculator();
		
		Integer result = calculator.execute(input);
		assertEquals(27, result);
	}
	
	@Test
	void testSuccessfulExecution2() {
		String input = "13, 5, +, 9, -";
		Calculator calculator = new Calculator();
		
		Integer result = calculator.execute(input);
		assertEquals(9, result);
	}
	
	@Test
	void testSuccessfulExecution3() {
		String input = "1, U, 2";
		Calculator calculator = new Calculator();
		
		Integer result = calculator.execute(input);
		assertEquals(2, result);
	}
	
	@Test
	void testSuccessfulExecution4() {
		String input = "1, 2, U, 3, +, U, U";
		Calculator calculator = new Calculator();
		
		Integer result = calculator.execute(input);
		assertEquals(1, result);
	}
	
	@Test
	void testFailedExecution() {
		String input = "13, 5, +, 9, -, U";
		Calculator calculator = new Calculator();
		
		IllegalStateException ex = assertThrows(
				IllegalStateException.class,
	            () -> calculator.execute(input)
	    );
	    assertEquals("The input or execution result is wrong.", ex.getMessage());
		
	}

}
