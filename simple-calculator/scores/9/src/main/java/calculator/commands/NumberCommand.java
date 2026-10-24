package calculator.commands;

public class NumberCommand implements Command {
	private Integer token;

	public NumberCommand(Integer token) {
		this.token = token;
	}

	public Integer execute() {
		return token;
	}
}
