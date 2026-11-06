package managers;

import entities.Employee;
import entities.Skill;
import entities.Task;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class SchedulerTest {
    @Test
    void wrapsAssignmentFailureWithoutRecordingTask() {
        EmployeeManager employees = new EmployeeManager() {
            @Override
            public void assignemployee(Employee employee, int workload) {
                throw new IllegalStateException("assignment failed");
            }
        };
        Employee employee = new Employee("employee", Set.of(Skill.A));
        employees.addEmployee(employee);
        TaskManager tasks = new TaskManager();
        ScheduleException error = assertThrows(ScheduleException.class,
                () -> new Scheduler(employees, tasks).scheduleTask(new Task(Skill.A, 4)));
        assertEquals("Failed to assign employee for task with skill A and load 4", error.getMessage());
        assertTrue(tasks.getTasksByEmployee(employee).isEmpty());
        assertEquals(0, employee.getLoad().get());
    }

    @Test
    void wrapsTaskRecordingFailure() {
        EmployeeManager employees = new EmployeeManager();
        employees.addEmployee(new Employee("employee", Set.of(Skill.A)));
        TaskManager tasks = new TaskManager() {
            @Override
            public void createTask(Task task, Employee employee) {
                throw new IllegalStateException("storage failed");
            }
        };
        ScheduleException error = assertThrows(ScheduleException.class,
                () -> new Scheduler(employees, tasks).scheduleTask(new Task(Skill.A, 4)));
        assertEquals("Failed to assign employee for task with skill A and load 4", error.getMessage());
    }

    @Test
    void reportIncludesTasksLoadsAndExtremes() throws Exception {
        EmployeeManager employees = new EmployeeManager();
        employees.addEmployee(new Employee("busy", Set.of(Skill.A)));
        employees.addEmployee(new Employee("idle", Set.of(Skill.B)));
        Scheduler scheduler = new Scheduler(employees, new TaskManager());
        Task task = new Task(Skill.A, 6);
        scheduler.scheduleTask(task);
        assertSame(employees.getEmployees().get(0), task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.ASSIGNED, task.getStatus());
        String report = report(scheduler);
        assertTrue(report.contains("Employee Load Report:"));
        assertTrue(report.contains("Employee ID: busy, Load: 6"));
        assertTrue(report.contains("Employee ID: idle, Load: 0"));
        assertTrue(report.contains("Task ID: " + task.getId() + ", Skill: A, Load: 6"));
        assertTrue(report.contains("Employee with Highest Load: Employee ID: busy, Load: 6"));
        assertTrue(report.contains("Employee with Lowest Load: " + System.lineSeparator()
                + "Employee ID: idle, Load: 0"));
    }

    @Test
    void reportHandlesEmptyTeam() {
        String report = report(new Scheduler(new EmployeeManager(), new TaskManager()));
        assertEquals(2, report.lines().filter(line -> line.contains("No employees available.")).count());
    }

    private String report(Scheduler scheduler) {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            scheduler.report();
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
