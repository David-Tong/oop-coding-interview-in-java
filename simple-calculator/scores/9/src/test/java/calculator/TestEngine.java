package calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EmptyStackException;
import java.util.List;

import org.junit.jupiter.api.Test;

import calculator.commands.AddCommand;
import calculator.commands.BinaryCommand;
import calculator.commands.NumberCommand;
import calculator.commands.SubCommand;
import calculator.commands.UndoCommand;

public class TestEngine {
	@Test
    void shouldRestoreStateWhenCommandFailsAfterMutation() {
        Engine engine = new Engine(List.of());
        engine.execute(new NumberCommand(7));

        // Addition pops 7 before failing to obtain its second operand.
        assertThrows(EmptyStackException.class,
                () -> engine.execute(new AddCommand()));
        assertEquals(7, engine.answer());
        assertEquals(7, engine.answer());

        // Exactly one successful operation remains in history.
        engine.execute(new UndoCommand());
        assertThrows(IllegalStateException.class, engine::answer);
        assertThrows(IllegalStateException.class,
                () -> engine.execute(new UndoCommand()));
    }

    @Test
    void shouldRestoreBothOperandsAndPreserveEarlierHistory() {
        Engine engine = new Engine(List.of());
        engine.execute(new NumberCommand(13));
        engine.execute(new NumberCommand(5));
        IllegalArgumentException failure = new IllegalArgumentException("Calculation failed");
        BinaryCommand failingCommand = new BinaryCommand() {
            @Override
            public Integer execute(Integer left, Integer right) {
                assertEquals(13, left);
                assertEquals(5, right);
                throw failure;
            }
        };

        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> engine.execute(failingCommand)));

        // Subtraction verifies that both values and their order were restored.
        engine.execute(new SubCommand());
        assertEquals(8, engine.answer());
        engine.execute(new UndoCommand());
        engine.execute(new UndoCommand());
        assertEquals(13, engine.answer());
        engine.execute(new UndoCommand());
        assertThrows(IllegalStateException.class, engine::answer);
        assertThrows(IllegalStateException.class,
                () -> engine.execute(new UndoCommand()));
    }

    @Test
    void shouldAcceptNewOperationsAfterBatchExecutionFails() {
        Engine engine = new Engine(List.of(new NumberCommand(7), new AddCommand()));

        assertThrows(EmptyStackException.class, engine::execute);
        assertEquals(7, engine.answer());

        // Continue through the single-operation API rather than replaying the batch.
        engine.execute(new NumberCommand(2));
        engine.execute(new SubCommand());
        assertEquals(5, engine.answer());
        engine.execute(new UndoCommand());
        engine.execute(new UndoCommand());
        assertEquals(7, engine.answer());
        engine.execute(new UndoCommand());
        assertThrows(IllegalStateException.class,
                () -> engine.execute(new UndoCommand()));
    }

    @Test
    void shouldAcceptAndUndoNewOperationsAfterEmptyHistoryUndoFails() {
        Engine engine = new Engine(List.of());

        assertThrows(IllegalStateException.class,
                () -> engine.execute(new UndoCommand()));
        engine.execute(new NumberCommand(9));
        assertEquals(9, engine.answer());
        engine.execute(new UndoCommand());
        assertThrows(IllegalStateException.class, engine::answer);
        assertThrows(IllegalStateException.class,
                () -> engine.execute(new UndoCommand()));

        engine.execute(new NumberCommand(4));
        assertEquals(4, engine.answer());
    }
}
