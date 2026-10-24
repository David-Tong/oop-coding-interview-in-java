package calculator.reviewer;

import calculator.ExecutionContext;
import calculator.commands.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Review the historical save-before-every-command protocol. */
class TestContextReview {
    private static void execute(ExecutionContext context, Command command) {
        context.save();
        command.execute(context);
    }
    @Test void failedArithmeticShouldNotAddHistory() {
        ExecutionContext context = new ExecutionContext();
        execute(context, new NumberCommand(7));
        assertThrows(IllegalStateException.class,
            () -> execute(context, new ArithmeticCommand("+")));
        assertEquals(7, context.getResult());
        context.undo();
        // Correct history would now restore the empty state, not another 7.
        assertThrows(IllegalStateException.class, () -> context.getResult());
    }
    @Test void partialFailureShouldRestoreOperands() {
        ExecutionContext context = new ExecutionContext();
        execute(context, new NumberCommand(7));
        Command failure = new Command() {
            @Override public void execute(ExecutionContext state) {
                state.pop();
                throw new IllegalStateException("simulated");
            }
        };
        assertThrows(IllegalStateException.class, () -> execute(context, failure));
        assertEquals(7, context.getResult());
    }
    @Test void undoCommandShouldNotDependOnExtraSave() {
        ExecutionContext context = new ExecutionContext();
        execute(context, new NumberCommand(1));
        execute(context, new NumberCommand(2));
        new UndoCommand().execute(context);
        assertEquals(1, context.getResult());
    }
    @Test void readingResultShouldNotConsumeIt() {
        ExecutionContext context = new ExecutionContext();
        execute(context, new NumberCommand(7));
        assertEquals(7, context.getResult());
        assertEquals(7, context.getResult());
    }
}
