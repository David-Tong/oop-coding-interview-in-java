package calculator;

import java.util.Stack;

public class ExecutionContext {
	class ExecutionStack {
		private Stack<Integer> stack;

		ExecutionStack() {
			this.stack = new Stack<>();
		}

		ExecutionStack(ExecutionStack other) {
			this.stack = new Stack<>();
			this.stack.addAll(other.stack);
		}

		@Override
		public String toString() {
			return stack.toString();
		}
	}

	private ExecutionStack current;
	private Stack<ExecutionStack> history;

	public ExecutionContext() {
		this.current = new ExecutionStack();
		this.history = new Stack<>();
		this.history.push(new ExecutionStack(current));
	}

	public void commit() {
		this.history.push(new ExecutionStack(current));
	}

	public void restore() {
		this.current = new ExecutionStack(this.history.peek());
	}

	public void undo() {
		if (this.history.size() <= 1) {
			throw new IllegalStateException("No operation to undo.");
		}
		this.history.pop();
		this.current = new ExecutionStack(this.history.peek());
	}

	public Integer popExecutionStack() {
		return this.current.stack.pop();
	}

	public void pushExecutionStack(Integer number) {
		this.current.stack.push(number);
	}

	public Integer fetchResult() {
		if (this.current.stack.size() == 1) {
			return this.current.stack.peek();
		} else {
			throw new IllegalStateException("The input or execution result is wrong.");
		}
	}

	@Override
	public String toString() {
		return String.format("Curent Execution Stack : %s, History Execution Stack : %s \n", this.current, this.history);
	}

}
