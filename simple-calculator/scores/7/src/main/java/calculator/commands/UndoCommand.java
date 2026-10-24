package calculator.commands;

import calculator.ExecutionContext;

public class UndoCommand extends Command {
	
	@Override
	public void execute(ExecutionContext context) {
		context.undo();
		context.undo();
	}

}
