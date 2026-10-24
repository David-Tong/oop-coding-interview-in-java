package calculator;

public class Main {

	public static void main(String[] args) {
		String input = "13, 5, +, 9, -, U, +";
		Calculator calculator = new Calculator(input);
		
		Integer result = calculator.execute();
		System.out.println("Result is %s".formatted(result));
	}

}
