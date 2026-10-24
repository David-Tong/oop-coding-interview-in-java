package calculator.helper;

import java.util.Arrays;
import java.util.List;

import calculator.commands.ArithmeticCommand;
import calculator.commands.Command;
import calculator.commands.NumberCommand;
import calculator.commands.UndoCommand;

public class CalculatorHelper {
	// 13, 5, +, 9 -, U, +
	public static List<Command> parse(String expression) {
		String[] tokens = expression.split(",");
		return Arrays.stream(tokens).map(String::trim).map(token -> {
			if (token.equals("+") || token.equals("-")) {
				return new ArithmeticCommand(token);
			}
			if (token.equals("U")) {
				return new UndoCommand();
			}
			if (token.matches("\\d+")) {
				return new NumberCommand(Integer.parseInt(token));
			}
			throw new IllegalArgumentException("Invalid token : " + token);
		}).toList();
	}
}
