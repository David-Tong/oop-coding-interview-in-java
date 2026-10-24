package calculator;

import java.util.List;

import calculator.commands.Command;
import calculator.helper.CalculatorHelper;

public class Calculator {	
	private Engine engine;
	private List<Command> commands;
	
	public Calculator(String expression) {
		this.commands = CalculatorHelper.parse(expression);
		this.engine = new Engine(this.commands);
	}
	
	public Integer execute() {
		engine.execute();
		return engine.answer();
	}
}
