package calculator.commands;

import calculator.ExecutionContext;

public abstract class Command {
	public abstract void execute(ExecutionContext context);
}
