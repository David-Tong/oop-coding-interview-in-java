package calculator.commands;

public abstract class BinaryCommand extends Command {
	public abstract Integer execute(Integer left, Integer right);
}
