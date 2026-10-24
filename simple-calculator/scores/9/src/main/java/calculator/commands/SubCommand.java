package calculator.commands;

public class SubCommand extends BinaryCommand {
	@Override
	public Integer execute(Integer left, Integer right) {
		return left - right;
	}
}
