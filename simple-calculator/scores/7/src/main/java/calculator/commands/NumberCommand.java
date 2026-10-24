package calculator.commands;

import calculator.ExecutionContext;


public class NumberCommand extends Command {
	private Integer token;
	
	public NumberCommand(Integer token) {
		this.token = token;
	}

	public void execute(ExecutionContext context) {
		context.push(token);
	}

}
