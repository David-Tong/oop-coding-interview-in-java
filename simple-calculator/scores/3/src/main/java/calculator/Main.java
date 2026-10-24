package calculator;
public class Main {
    public static void main(String[] args) {
        String input = args.length == 0 ? "13, 5, +" : args[0];
        System.out.println(new Calculator(input).execute());
    }
}
