package calculator.helper;

import java.util.Arrays;
import java.util.List;

import calculator.commands.AddCommand;
import calculator.commands.Command;
import calculator.commands.NumberCommand;
import calculator.commands.SubCommand;
import calculator.commands.UndoCommand;

public class CalculatorHelper {
	// 13, 5, +, 9 -, U, +
	public static List<Command> parse(String expression) {
		String[] tokens = expression.split(",");
		return Arrays.stream(tokens).map(String::trim).map(token -> {
			if (token.equals("+")) {
				return new AddCommand();
			}
			if (token.equals("-")) {
				return new SubCommand();
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
