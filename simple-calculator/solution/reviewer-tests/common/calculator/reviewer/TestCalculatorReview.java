package calculator.reviewer;

import calculator.Calculator;


import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Correct specification assertions; intentionally incomplete versions fail some. */
class TestCalculatorReview {
    @Test void shouldSubtractInCorrectOrder() {
        assertEquals(8, new Calculator("13, 5, -").execute());
    }
    @Test void shouldUndoArithmetic() {
        assertEquals(27, new Calculator("13, 5, +, 9, -, U, +").execute());
    }
    @Test void shouldUndoFirstNumber() {
        assertEquals(2, new Calculator("1, U, 2").execute());
    }
    @Test void shouldUndoConsecutively() {
        assertEquals(1, new Calculator("1, 2, +, U, U").execute());
    }
    @Test void shouldPreserveHistoryAfterBranching() {
        assertEquals(1, new Calculator("1, 2, U, 3, +, U, U").execute());
    }
}
