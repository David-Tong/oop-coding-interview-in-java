package calculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.IntStream;

import calculator.commands.Command;
import calculator.commands.UndoCommand;

public class Engine {
	
	class ExecutionContext {
		private Stack<Integer> stack;
		
		public ExecutionContext() {
			this.stack = new Stack<>();
		}
		
		public ExecutionContext(ExecutionContext other) {
			this.stack = new Stack<>();
			this.stack.addAll(other.stack);
		}
		
		private void push(Integer number) {
			this.stack.push(number);
		}	
		
		private Integer pop() {
			return this.stack.pop();
		}
		
		private List<Integer> fetchOperands(Integer size) {
			List<Integer> operands = new ArrayList<>();
			if (this.stack.size() < size) {
				throw new IllegalStateException("The execution stack state is abnormal.");
			}
			IntStream.range(0, size)
	         .forEach(idx -> operands.add(this.stack.pop()));
			return operands;
		}
			
		private Integer fetchResult() {
			if (this.stack.size() == 1) {
				return this.stack.peek();
			} else {
				throw new IllegalStateException("The input or execution result is wrong.");
			}
		}
		
		@Override
		public String toString() {
			return this.stack.toString();
		}
	}
	
	private Stack<ExecutionContext> history;
	private ExecutionContext current;
	
	public Engine() {
		this.history = new Stack<>();
		this.current = new ExecutionContext();
		this.history.push(new ExecutionContext(current));
	}
	
	private void save() {
		this.history.push(new ExecutionContext(current));
	}
	
	private void undo() {
		if (history.size() <= 1) {
		    throw new IllegalStateException("No operation to undo.");
		}
		
		this.history.pop();
		this.current = new ExecutionContext(this.history.peek());
	}
	
	private void restore() {
		this.current = new ExecutionContext(this.history.peek());
	}

	public void execute(Command command) {
		if (command instanceof UndoCommand) {
			this.undo();
		} else {
			try {
				command.execute(this);
				this.save();
			} catch(Exception ex) {
				this.restore();
				throw ex; 
			}
		}
	}
	
	public void push(Integer number) {
		 this.current.push(number);
	}
	
	public Integer pop() {
		return this.current.pop();
	}
	
	public List<Integer> fetchOperands(Integer size) {
		return this.current.fetchOperands(size);
	}
	
	public Integer getResult() {
		return this.current.fetchResult();
	}
}
