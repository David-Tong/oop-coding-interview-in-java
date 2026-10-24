package calculator.helper;

import calculator.Calculator;
import calculator.helper.CalculatorHelper;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import calculator.commands.Command;

public class TestCalculatorHelper {
	
	@Test
	void testSuccessfulParse() {
		String expression = "13, 5, +, 9, -, U, +";
		List<Command> commands = CalculatorHelper.parse(expression);
		assertEquals(7, commands.size());
	}
	
	@Test
	void testFailedParse() {
		String expression = "13, 5, +, 9-, U, +";
		IllegalArgumentException ex = assertThrows(
	            IllegalArgumentException.class,
	            () -> CalculatorHelper.parse(expression)
	    );
	    assertEquals("Invalid token : 9-", ex.getMessage());
	}
	
}
