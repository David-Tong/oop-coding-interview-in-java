package calculator;

import java.util.List;

import calculator.commands.Command;
import calculator.helper.CalculatorHelper;

public class Calculator {	
	private ExecutionContext context;
	private List<Command> commands;
	
	public Calculator(String expression) {
		this.context = new ExecutionContext();
		this.commands = CalculatorHelper.parse(expression);
	}
	
	public Integer execute() {
		for (Command command : this.commands) {
			context.save();
			command.execute(context);
		}
		return context.getResult();
	}
}
