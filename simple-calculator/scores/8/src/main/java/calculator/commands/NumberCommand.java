package calculator.commands;

import calculator.Engine;


public class NumberCommand extends Command {
	private Integer token;
	
	public NumberCommand(Integer token) {
		this.token = token;
	}

	public void execute(Engine context) {
		context.push(token);
	}

}
