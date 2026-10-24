package calculator.commands;

public class AddCommand extends BinaryCommand {
	@Override
	public Integer execute(Integer left, Integer right) {
		return left + right;
	}
}
