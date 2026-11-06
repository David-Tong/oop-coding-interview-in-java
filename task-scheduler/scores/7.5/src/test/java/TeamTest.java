import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class TeamTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    @Timeout(40)
    void mainProcessesAllTasksPrintsReportAndTerminatesWorkers() throws Exception {
        // A subprocess bounds execution even if main hangs or leaks executor threads.
        Path output = temporaryDirectory.resolve("team.log");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classes = Path.of(Team.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        Process process = new ProcessBuilder(java, "-cp", classes, "Team")
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Team must shut down its executor and exit");
            assertEquals(0, process.exitValue());
            List<String> lines = Files.readAllLines(output);
            long scheduled = lines.stream().filter(line -> line.startsWith("Scheduled task: ")).count();
            long failed = lines.stream().filter(line -> line.startsWith("Failed to schedule task: ")).count();
            assertEquals(5000, scheduled + failed);
            assertTrue(scheduled > 0);
            assertEquals(scheduled, lines.stream().filter(line -> line.startsWith("Task ID: ")).count());
            assertTrue(lines.contains("Employee Load Report:"));
            for (int id = 1; id <= 5; id++) {
                String prefix = "Employee ID: " + id + ", Load: ";
                assertTrue(lines.stream().anyMatch(line -> line.startsWith(prefix)), prefix);
            }
            assertTrue(lines.stream().anyMatch(line -> line.startsWith("Employee with Highest Load:")));
            assertTrue(lines.stream().anyMatch(line -> line.startsWith("Employee with Lowest Load:")));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                assertTrue(process.waitFor(5, TimeUnit.SECONDS));
            }
        }
    }
}
