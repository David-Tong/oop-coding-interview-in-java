package calculator;

import calculator.Calculator;
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

    // Restored historical omission: this method has no @Test.
    void testFailedExecution() {
        String input = "13, 5, +, 9, -, U";
        Calculator calculator = new Calculator(input);
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class, () -> calculator.execute());
        assertEquals("The input or execution result is wrong.", ex.getMessage());
    }
}
