package calculator;

import calculator.Calculator;
import calculator.Engine;
import calculator.helper.CalculatorHelper;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import calculator.commands.Command;
import calculator.commands.NumberCommand;

public class TestEngine {
	@Test
    void shouldRestoreStateWhenCommandFailsAfterMutation() {
        Engine engine = new Engine();
        engine.execute(new NumberCommand(7));

        Command failingCommand = new Command() {
            @Override
            public void execute(Engine context) {
                context.pop(); // 先移除 7，模拟部分执行
                throw new IllegalStateException("Simulated failure");
            }
        };

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> engine.execute(failingCommand)
        );

        assertEquals("Simulated failure", exception.getMessage());
        assertEquals(7, engine.getResult());
    }
}
