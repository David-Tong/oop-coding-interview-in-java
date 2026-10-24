package calculator.commands;

public abstract class BinaryCommand implements Command {
	public abstract Integer execute(Integer left, Integer right);
}
