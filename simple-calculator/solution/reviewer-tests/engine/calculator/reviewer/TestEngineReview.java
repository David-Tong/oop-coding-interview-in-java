package calculator.reviewer;

import calculator.Calculator;
import calculator.Engine;
import calculator.helper.CalculatorHelper;


import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import calculator.commands.*;

/** Added teaching regressions; failures expose defects, not expected behavior. */
class TestEngineReview {
    @Test void shouldRecoverAfterEmptyUndo() {
        Engine engine = new Engine();
        assertThrows(IllegalStateException.class,
            () -> engine.execute(new UndoCommand()));
        engine.execute(new NumberCommand(9));
        engine.execute(new UndoCommand());
        assertThrows(IllegalStateException.class, () -> engine.getResult());
    }
    @Test void shouldRestoreStateAfterPartialFailure() {
        Engine engine = new Engine();
        engine.execute(new NumberCommand(7));
        IllegalStateException failure = new IllegalStateException("simulated");
        Command command = new Command() {
            @Override public void execute(Engine context) {
                context.pop();
                throw failure;
            }
        };
        assertSame(failure, assertThrows(IllegalStateException.class,
            () -> engine.execute(command)));
        assertEquals(7, engine.getResult());
        engine.execute(new UndoCommand());
        assertThrows(IllegalStateException.class, () -> engine.getResult());
        assertThrows(IllegalStateException.class,
            () -> engine.execute(new UndoCommand()));
    }
}
