package calculator.commands;

import java.util.List;

import calculator.ExecutionContext;

public class ArithmeticCommand extends Command {
	private String token;
	private final Integer OPERAND_SIZE = 2;
	
	public ArithmeticCommand(String token) {
		this.token = token;
	}
	
	@Override
	public void execute(ExecutionContext context) {
		List<Integer> operands = context.fetchOperands(OPERAND_SIZE);
		Integer operand = operands.get(0);
		Integer operand2 = operands.get(1);
		
		if (token.equals("+")) {
			context.push(operand2 + operand);
		} else if (token.equals("-")) {
			context.push(operand2 - operand);
		}
	}
}
