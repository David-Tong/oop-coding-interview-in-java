package calculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.IntStream;

public class ExecutionContext {
	class ExecutionStack {
		private Stack<Integer> stack;
		
		public ExecutionStack() {
			this.stack = new Stack<>();
		}
		
		public ExecutionStack(ExecutionStack other) {
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
		
	}
	
    private Stack<ExecutionStack> history;
    private ExecutionStack current;

    public ExecutionContext() {
        this.history = new Stack<>();
        this.current = new ExecutionStack();
    }

    public void save() {
        this.history.push(new ExecutionStack(current));
    }

    public void undo() {
        this.current = this.history.pop();
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
