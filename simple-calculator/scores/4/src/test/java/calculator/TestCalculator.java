package calculator;

import calculator.Calculator;


import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Basic tests for the newly written teaching implementation. */
class TestCalculator {
    @Test void shouldAdd() {
        assertEquals(18, new Calculator("13, 5, +").execute());
    }
    @Test void shouldRejectMultipleResults() {
        assertThrows(IllegalStateException.class,
            () -> new Calculator("1, 2").execute());
    }
}
