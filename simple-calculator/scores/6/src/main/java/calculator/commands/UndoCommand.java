package calculator.commands;

import java.util.Stack;

public class UndoCommand extends Command {
    @Override
    public Stack<Integer> execute(Stack<Command> engine, Stack<Integer> snapshot) {
        return engine.pop().undo();
    }
}
