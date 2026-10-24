"""Compile each teaching implementation separately and check documented behavior.
Requires Python 3 and JDK 17+. No Maven or external dependencies.
This is a characterization suite, not a claim that defective versions pass the problem.
"""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parent
PROBE = r"""
import calculator.Calculator;
public class ScoreProbe {
    static void expression(String name, String input) {
        try { System.out.println(name + "=" + new Calculator(input).execute()); }
        catch (RuntimeException e) { System.out.println(name + "=" + e.getClass().getSimpleName()); }
    }
    public static void main(String[] args) {
        expression("add", "13, 5, +");
        expression("subtract", "13, 5, -");
        expression("undo", "13, 5, +, 9, -, U, +");
        expression("first", "1, U, 2");
        expression("branch", "1, 2, U, 3, +, U, U");
        expression("consecutive", "1, 2, +, U, U");
        expression("trailing", "1,");
        Calculator c = new Calculator("1");
        c.execute();
        try { System.out.println("repeat=" + c.execute()); }
        catch (RuntimeException e) { System.out.println("repeat=" + e.getClass().getSimpleName()); }
    }
}
"""
ENGINE_PROBE = r"""
import calculator.Engine;
import calculator.commands.*;
public class EngineProbe {
    public static void main(String[] args) {
        Engine engine = new Engine();
        engine.execute(new NumberCommand(7));
        RuntimeException failure = new IllegalStateException("simulated");
        try {
            engine.execute(new Command() {
                public void execute(Engine e) { e.pop(); throw failure; }
            });
        } catch (RuntimeException e) {
            System.out.println("original=" + (e == failure));
        }
        try { System.out.println("rollback=" + engine.getResult()); }
        catch (RuntimeException e) { System.out.println("rollback=" + e.getClass().getSimpleName()); }
        Engine empty = new Engine();
        try { empty.execute(new UndoCommand()); }
        catch (RuntimeException e) { System.out.println("empty=" + e.getClass().getSimpleName()); }
        empty.execute(new NumberCommand(9));
        try { empty.execute(new UndoCommand()); System.out.println("recovery=ok"); }
        catch (RuntimeException e) { System.out.println("recovery=" + e.getClass().getSimpleName()); }
    }
}
"""
EXPECTED = {
    3: "18,-8,EmptyStackException,IllegalStateException,IllegalStateException,3,1,1",
    4: "18,8,27,2,IllegalStateException,IllegalStateException,1,1",
    6: "18,-8,27,2,1,1,1,1",
    7: "18,8,27,2,1,1,1,IllegalStateException",
    8: "18,8,27,2,1,1,1,IllegalStateException",
}

def run_java(output, name):
    result = subprocess.run(["java", "-cp", str(output), name], check=True,
                            capture_output=True, text=True)
    return dict(line.split("=", 1) for line in result.stdout.splitlines())

def main():
    for score, expected in EXPECTED.items():
        with tempfile.TemporaryDirectory(prefix=f"calculator-score-{score}-") as tmp:
            output = Path(tmp)
            probe = output / "ScoreProbe.java"
            probe.write_text(PROBE, encoding="utf-8")
            sources = list((ROOT / str(score) / "src/main/java").rglob("*.java"))
            if score == 8:
                engine_probe = output / "EngineProbe.java"
                engine_probe.write_text(ENGINE_PROBE, encoding="utf-8")
                sources.append(engine_probe)
            subprocess.run(["javac", "--release", "17", "-encoding", "UTF-8",
                            "-d", str(output), *map(str, sources), str(probe)], check=True)
            actual = run_java(output, "ScoreProbe")
            assert list(actual.values()) == expected.split(","), (score, actual)
            print(f"{score}: {actual}")
            if score == 8:
                engine = run_java(output, "EngineProbe")
                expected_engine = {
                    "original": "true",
                    "rollback": "7" if score == 8 else "IllegalStateException",
                    "empty": "EmptyStackException" if score == 6 else "IllegalStateException",
                    "recovery": "EmptyStackException" if score == 6 else "ok",
                }
                assert engine == expected_engine, (score, engine)
                print(f"   {engine}")
    print("All documented behaviors verified (including intentional failures).")

if __name__ == "__main__":
    main()
