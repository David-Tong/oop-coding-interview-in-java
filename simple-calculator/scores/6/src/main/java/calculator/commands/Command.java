package calculator.commands;

import java.util.Stack;

public abstract class Command {
    Stack<Integer> snapshot;
    public abstract Stack<Integer> execute(Stack<Command> engine, Stack<Integer> snapshot);
    public Stack<Integer> undo() {
        return this.snapshot;
    }
}
