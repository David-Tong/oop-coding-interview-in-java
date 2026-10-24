package calculator;

import java.util.List;

import calculator.commands.BinaryCommand;
import calculator.commands.Command;
import calculator.commands.NumberCommand;
import calculator.commands.UndoCommand;

public class Engine {

	private ExecutionContext context;
	private List<Command> commands;

	public Engine(List<Command> commands) {
		context = new ExecutionContext();
		this.commands = commands;
	}

	public void execute() {
		for (Command command : this.commands) {
			execute(command);
		}
	}

	public void execute(Command command) {
		try {
			if (command instanceof UndoCommand) {
				this.context.undo();
				return;
			} else {
				if (command instanceof BinaryCommand) {
					Integer right = this.context.popExecutionStack();
					Integer left = this.context.popExecutionStack();
					Integer result = ((BinaryCommand) command).execute(left, right);
					this.context.pushExecutionStack(result);
				} else if (command instanceof NumberCommand) {
					Integer result = ((NumberCommand) command).execute();
					this.context.pushExecutionStack(result);
				} else {
					throw new IllegalArgumentException("Unsupported command: " + command.getClass().getName());
				}		
			}
			// System.out.print(context);
		} catch (Exception ex) {
			this.context.restore();
			throw ex;
		}
		this.context.commit();
	}

	public Integer answer() {
		return this.context.fetchResult();
	}

}
