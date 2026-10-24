package calculator;

import java.util.Stack;

/** Intentionally incomplete teaching example; see README.md. */
public class Calculator {
    private final String expression;

    public Calculator(String expression) {
        this.expression = expression;
    }

    public Integer execute() {
        Stack<Integer> current = new Stack<>();
        Stack<Integer> previous = new Stack<>();
        for (String raw : expression.split(",")) {
            String token = raw.trim();
            if (token.equals("U")) {
                // Incorrect: accepts undo but does nothing.
                continue;
            }
            previous = new Stack<>();
            previous.addAll(current);
            if (token.equals("+") || token.equals("-")) {
                int right = current.pop();
                int left = current.pop();
                current.push(token.equals("+") ? left + right : right - left);
            } else {
                current.push(Integer.parseInt(token));
            }
        }
        if (current.size() != 1) {
            throw new IllegalStateException("Expected exactly one result.");
        }
        return current.peek();
    }
}
