package calculator.commands;

public class AddCommand extends BinaryCommand {
	@Override
	public Integer execute() {
		throw new UnsupportedOperationException("NinaryCommand doesn't support execute without arguments.");
	}

	@Override
	public Integer execute(Integer left, Integer right) {
		return left + right;
	}
}
