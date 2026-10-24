package calculator;

import java.util.List;
import java.util.Stack;
import calculator.commands.Command;
import calculator.helper.CalculatorHelper;

public class Calculator {
    private Stack<Command> engine;
    private Stack<Integer> current;
    private List<Command> commands;

    public Calculator(String expression) {
        this.engine = new Stack<Command>();
        this.current = new Stack<Integer>();
        this.commands = CalculatorHelper.parse(expression);
    }

    public Integer execute() {
        for (Command command : this.commands) {
            current = command.execute(engine, current);
        }
        if (current.size() == 1) {
            return current.pop();
        } else {
            throw new IllegalStateException("The input or execution result is wrong.");
        }
    }
}
