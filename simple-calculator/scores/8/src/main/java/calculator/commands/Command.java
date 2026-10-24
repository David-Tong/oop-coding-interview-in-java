package calculator.commands;

import calculator.Engine;

public abstract class Command {
	public abstract void execute(Engine context);
}
