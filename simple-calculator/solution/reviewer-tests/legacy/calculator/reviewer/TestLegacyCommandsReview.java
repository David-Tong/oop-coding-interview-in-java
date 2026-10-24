package calculator.reviewer;

import calculator.TestCalculator;
import calculator.commands.*;
import java.lang.reflect.*;
import java.util.Stack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Review legacy API without adding Engine or changing author assertions. */
class TestLegacyCommandsReview {
    @Test void shouldPreserveStateAndHistoryWhenArithmeticFails() {
        Stack<Command> history = new Stack<>();
        Stack<Integer> current = new Stack<>();
        new NumberCommand(7).execute(history, current);
        assertThrows(RuntimeException.class,
            () -> new ArithmeticCommand("+").execute(history, current));
        assertEquals(java.util.List.of(7), current);
        assertEquals(1, history.size());
    }
    @Test void historicalFailureAssertionShouldMatchActualException() throws Exception {
        // Explicitly exercise the historical method that JUnit cannot discover.
        Method method = TestCalculator.class.getDeclaredMethod("testFailedExecution");
        method.setAccessible(true);
        try {
            method.invoke(new TestCalculator());
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Error) throw (Error) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw ex;
        }
    }
}
