package calculator;

import java.util.List;

import calculator.commands.Command;
import calculator.helper.CalculatorHelper;

public class Calculator {	
	private Engine context;
	private List<Command> commands;
	
	public Calculator(String expression) {
		this.context = new Engine();
		this.commands = CalculatorHelper.parse(expression);
	}
	
	public Integer execute() {
		for (Command command : this.commands) {
			context.execute(command);
		}
		return context.getResult();
	}
}
