package calculator;

import java.util.List;

import calculator.commands.Command;
import calculator.helper.CalculatorHelper;

public class Calculator {	
	public Integer execute(String expression) {
		List<Command> commands = CalculatorHelper.parse(expression);
		Engine engine = new Engine(commands);
		engine.execute();
		return engine.answer();
	}
}
