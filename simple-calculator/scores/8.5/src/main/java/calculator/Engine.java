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
		for(Command command : this.commands) {
			try {
				if (command instanceof UndoCommand) {
					this.context.undo();
				} else {
					if (command instanceof BinaryCommand) {
						Integer right = this.context.popExecutionStack();
						Integer left = this.context.popExecutionStack();
						Integer result = ((BinaryCommand) command).execute(left, right);
						this.context.pushExecutionStack(result);
					}
					if (command instanceof NumberCommand) {
						Integer result = command.execute();
						this.context.pushExecutionStack(result);
					}
					this.context.commit();		
				}
				// System.out.print(context);
			} catch (Exception ex) {
				this.context.restore();
				throw ex;
			}
		}
	}
	
	public Integer answer() {
		return this.context.fetchResult();
	}
	
}
