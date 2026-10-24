package calculator.commands;

import java.util.Stack;

public class ArithmeticCommand extends Command {
    private String token;
    public ArithmeticCommand(String token) {
        this.token = token;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stack<Integer> execute(Stack<Command> engine, Stack<Integer> current) {
        this.snapshot = (Stack<Integer>) current.clone();
        engine.add(this);
        Integer operand = current.pop();
        Integer operand2 = current.pop();
        if (this.token.equals("+")) {
            current.add(operand + operand2);
        } else if (this.token.equals("-")) {
            current.add(operand - operand2);
        }
        return current;
    }
}
