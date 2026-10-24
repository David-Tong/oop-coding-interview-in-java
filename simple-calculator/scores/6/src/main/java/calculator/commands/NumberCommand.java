package calculator.commands;

import java.util.Stack;

public class NumberCommand extends Command {
    private Integer token;
    public NumberCommand(Integer token) {
        this.token = token;
    }

    @SuppressWarnings("unchecked")
    public Stack<Integer> execute(Stack<Command> engine, Stack<Integer> current) {
        this.snapshot = (Stack<Integer>) current.clone();
        engine.add(this);
        current.add(token);
        return current;
    }
}
