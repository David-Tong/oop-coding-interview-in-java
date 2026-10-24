package calculator.reviewer;

import calculator.Calculator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Additional requirements from the current problem statement. */
class TestInputContract {
    @Test void shouldRejectTrailingEmptyToken() {
        assertThrows(IllegalArgumentException.class,
            () -> new Calculator("1,").execute());
    }
    @Test void shouldDefineRepeatedExecutionContract() {
        Calculator calculator = new Calculator("1");
        assertEquals(1, calculator.execute());
        // This test chooses repeatable evaluation. A documented one-shot API
        // should replace this with a precise lifecycle-exception assertion.
        assertEquals(1, calculator.execute());
    }
}
