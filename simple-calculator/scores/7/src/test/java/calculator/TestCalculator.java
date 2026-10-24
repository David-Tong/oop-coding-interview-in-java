package calculator;

import calculator.Calculator;
import calculator.helper.CalculatorHelper;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TestCalculator {
	
	@Test
	void testSuccessfulExecution() {
		String input = "13, 5, +, 9, -, U, +";
		Calculator calculator = new Calculator(input);
		
		Integer result = calculator.execute();
		assertEquals(27, result);
	}
	
	@Test
	void testSuccessfulExecution2() {
		String input = "13, 5, +, 9, -";
		Calculator calculator = new Calculator(input);
		
		Integer result = calculator.execute();
		assertEquals(9, result);
	}
	
	@Test
	void testFailedExecution() {
		String input = "13, 5, +, 9, -, U";
		Calculator calculator = new Calculator(input);
		
		IllegalStateException ex = assertThrows(
				IllegalStateException.class,
	            () -> calculator.execute()
	    );
	    assertEquals("The input or execution result is wrong.", ex.getMessage());
		
	}

}
