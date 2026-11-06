package scheduling;
import employee.EmployeeManager;
import task.TaskManager;

import employee.Employee;
import employee.Skill;
import task.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class SchedulerTest {
    @ParameterizedTest(name = "checked={0}, rollback fails after restoring load={1}")
    @CsvSource({"true, false", "true, true", "false, false", "false, true"})
    void rollbackExceptionReplacesRecordingException(boolean checked, boolean afterRollback) {
        ScheduleException checkedFailure = new ScheduleException("recording failed");
        RuntimeException runtimeFailure = new IllegalStateException("recording failed");
        RuntimeException rollbackFailure = new IllegalStateException("rollback failed");
        AtomicInteger rollbacks = new AtomicInteger();
        Employee employee = new Employee("employee", Set.of(Skill.A));
        EmployeeManager employees = new EmployeeManager() {
            @Override
            public Employee rollbackAssignment(Employee selected, Task task) {
                assertSame(employee, selected);
                assertEquals(4, task.getLoad());
                rollbacks.incrementAndGet();
                if (afterRollback) super.rollbackAssignment(selected, task);
                throw rollbackFailure;
            }
        };
        employees.addEmployee(employee);
        TaskManager tasks = new TaskManager() {
            @Override
            public void assignTask(Task task, Employee selected) throws ScheduleException {
                assertEquals(4, selected.getLoad());
                if (checked) throw checkedFailure;
                throw runtimeFailure;
            }
        };
        Task task = new Task(Skill.A, 4);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> new Scheduler(employees, tasks).scheduleTask(task));

        // Characterize current behavior: rollback exceptions escape the catch block.
        assertSame(rollbackFailure, thrown);
        assertNull(thrown.getCause());
        assertEquals(0, thrown.getSuppressed().length);
        assertEquals(1, rollbacks.get());
        assertEquals(afterRollback ? 0 : 4, employee.getLoad());
        assertNull(task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.PENDING, task.getStatus());
        assertTrue(tasks.getTasksByEmployee(employee).isEmpty());
    }

    @ParameterizedTest(name = "Error during recording={0}")
    @CsvSource({"false", "true"})
    void javaErrorsCurrentlyPropagateWithoutRollback(boolean duringRecording) {
        AssertionError injected = new AssertionError("injected error");
        AtomicInteger rollbacks = new AtomicInteger();
        AtomicInteger recordingCalls = new AtomicInteger();
        Employee employee = new Employee("employee", Set.of(Skill.A));
        EmployeeManager employees = new EmployeeManager() {
            @Override
            public Employee assignEmployee(Task task) throws ScheduleException {
                if (!duringRecording) throw injected;
                return super.assignEmployee(task);
            }

            @Override
            public Employee rollbackAssignment(Employee selected, Task task) {
                rollbacks.incrementAndGet();
                return super.rollbackAssignment(selected, task);
            }
        };
        employees.addEmployee(employee);
        TaskManager tasks = new TaskManager() {
            @Override
            public void assignTask(Task task, Employee selected) {
                recordingCalls.incrementAndGet();
                assertEquals(4, selected.getLoad());
                throw injected;
            }
        };
        Task task = new Task(Skill.A, 4);

        assertSame(injected, assertThrows(AssertionError.class,
                () -> new Scheduler(employees, tasks).scheduleTask(task)));
        assertEquals(0, rollbacks.get(), "Error is outside the existing Exception handlers");
        assertEquals(duringRecording ? 1 : 0, recordingCalls.get());
        assertEquals(duringRecording ? 4 : 0, employee.getLoad());
        assertNull(task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.PENDING, task.getStatus());
        assertTrue(tasks.getTasksByEmployee(employee).isEmpty());
    }

    @ParameterizedTest(name = "checked failure after recording={0}")
    @CsvSource({"true", "false"})
    void failureAfterRecordingRollsBackWorkloadButKeepsTaskRecord(boolean checked) {
        ScheduleException checkedFailure = new ScheduleException("failed after recording");
        RuntimeException runtimeFailure = new NullPointerException("failed after recording");
        Employee employee = new Employee("employee", Set.of(Skill.A));
        EmployeeManager employees = new EmployeeManager();
        employees.addEmployee(employee);
        TaskManager tasks = new TaskManager() {
            @Override
            public void assignTask(Task task, Employee selected) throws ScheduleException {
                super.assignTask(task, selected);
                if (checked) throw checkedFailure;
                throw runtimeFailure;
            }
        };
        Task task = new Task(Skill.A, 4);

        Exception thrown = assertThrows(Exception.class,
                () -> new Scheduler(employees, tasks).scheduleTask(task));

        assertSame(checked ? checkedFailure : runtimeFailure, thrown);
        assertEquals(0, employee.getLoad());
        // The current handler does not undo a task record that was already written.
        assertEquals(java.util.List.of(task), tasks.getTasksByEmployee(employee));
        assertSame(employee, task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.ASSIGNED, task.getStatus());
    }

    @Test
    void noMatchingEmployeeSkipsRecordingAndRollback() {
        EmployeeManager employees = new EmployeeManager() {
            @Override
            public Employee rollbackAssignment(Employee selected, Task task) {
                return fail("No assignment was made, so nothing should be rolled back");
            }
        };
        TaskManager tasks = new TaskManager() {
            @Override
            public void assignTask(Task task, Employee selected) {
                fail("No employee was selected");
            }
        };
        Task task = new Task(Skill.A, 4);
        ScheduleException thrown = assertThrows(ScheduleException.class,
                () -> new Scheduler(employees, tasks).scheduleTask(task));
        assertEquals("No available employee for task with skill A and load 4", thrown.getMessage());
        assertNull(task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.PENDING, task.getStatus());
    }

    @Test
    void invalidTasksAreRejectedBeforeCallingEitherManager() {
        EmployeeManager employees = new EmployeeManager() {
            @Override
            public Employee assignEmployee(Task task) {
                fail("Invalid task must not reach employee assignment");
                return null;
            }
        };
        TaskManager tasks = new TaskManager() {
            @Override
            public void assignTask(Task task, Employee employee) {
                fail("Invalid task must not reach task recording");
            }
        };
        Scheduler scheduler = new Scheduler(employees, tasks);
        for (Task task : new Task[]{null, new Task(null, 1), new Task(Skill.A, 0), new Task(Skill.A, -1)}) {
            ScheduleException error = assertThrows(ScheduleException.class, () -> scheduler.scheduleTask(task));
            assertEquals("Task cannot be null or have invalid skill or load", error.getMessage());
        }
    }

    @ParameterizedTest(name = "checked exception={0}, failure during recording={1}")
    @CsvSource({"true, false", "true, true", "false, false", "false, true"})
    void recordingFailuresRollbackAndAllowRetry(boolean checked, boolean duringRecording)
            throws Exception {
        ScheduleException checkedFailure = new ScheduleException("injected scheduling failure");
        IllegalStateException runtimeFailure = new IllegalStateException("injected runtime failure");
        AtomicInteger rollbacks = new AtomicInteger();
        Task failedTask = new Task(Skill.A, 4);
        Employee employee = new Employee("employee", Set.of(Skill.A, Skill.B));
        Employee competitor = new Employee("competitor", Set.of(Skill.A, Skill.B));
        EmployeeManager employees = new EmployeeManager() {
            @Override
            public Employee assignEmployee(Task task) throws ScheduleException {
                if (task == failedTask && !duringRecording) {
                    if (checked) throw checkedFailure;
                    throw runtimeFailure;
                }
                return super.assignEmployee(task);
            }

            @Override
            public Employee rollbackAssignment(Employee selected, Task task) {
                assertSame(employee, selected);
                assertEquals(4, task.getLoad());
                rollbacks.incrementAndGet();
                return super.rollbackAssignment(selected, task);
            }
        };
        employees.addEmployee(competitor);
        employees.assignEmployee(new Task(Skill.A, 3));
        employees.addEmployee(employee);
        employees.assignEmployee(new Task(Skill.A, 2));
        TaskManager tasks = new TaskManager() {
            @Override
            public void assignTask(Task task, Employee selected) throws ScheduleException {
                if (task == failedTask && duringRecording) {
                    assertSame(employee, selected);
                    assertEquals(6, selected.getLoad(), "Inject after workload was increased");
                    if (checked) throw checkedFailure;
                    throw runtimeFailure;
                }
                super.assignTask(task, selected);
            }
        };
        Scheduler scheduler = new Scheduler(employees, tasks);

        Exception error = assertThrows(Exception.class,
                () -> scheduler.scheduleTask(failedTask));

        assertSame(checked ? checkedFailure : runtimeFailure, error);
        assertEquals(duringRecording ? 1 : 0, rollbacks.get());
        int expectedLoad = 2;
        assertEquals(expectedLoad, employee.getLoad());
        assertEquals(3, competitor.getLoad());
        assertNull(failedTask.getAssignedEmployee());
        assertEquals(Task.TaskStatus.PENDING, failedTask.getStatus());
        assertTrue(tasks.getTasksByEmployee(employee).isEmpty());
        assertTrue(tasks.getTasksByEmployee(competitor).isEmpty());
        Employee expectedNext = expectedLoad < competitor.getLoad() ? employee : competitor;
        for (Skill skill : Set.of(Skill.A, Skill.B)) {
            assertSame(expectedNext, employees.assignEmployee(new Task(skill, 0)));
        }
        Task retry = new Task(Skill.B, 1);
        scheduler.scheduleTask(retry);
        assertSame(expectedNext, retry.getAssignedEmployee());
        assertEquals(Task.TaskStatus.ASSIGNED, retry.getStatus());
        assertEquals(java.util.List.of(retry), tasks.getTasksByEmployee(expectedNext));
        assertEquals(Math.min(expectedLoad, 3) + 1, expectedNext.getLoad());
    }

    @Test
    void propagatesAssignmentRuntimeFailureWithoutRecordingTask() {
        EmployeeManager employees = new EmployeeManager() {
            @Override
            public Employee assignEmployee(Task task) {
                throw new IllegalStateException("assignment failed");
            }
        };
        Employee employee = new Employee("employee", Set.of(Skill.A));
        employees.addEmployee(employee);
        TaskManager tasks = new TaskManager();
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new Scheduler(employees, tasks).scheduleTask(new Task(Skill.A, 4)));
        assertEquals("assignment failed", error.getMessage());
        assertTrue(tasks.getTasksByEmployee(employee).isEmpty());
        assertEquals(0, employee.getLoad());
    }

    @Test
    void propagatesTaskRecordingRuntimeFailureAfterRollback() {
        EmployeeManager employees = new EmployeeManager();
        employees.addEmployee(new Employee("employee", Set.of(Skill.A)));
        TaskManager tasks = new TaskManager() {
            @Override
            public void assignTask(Task task, Employee employee) {
                throw new IllegalStateException("storage failed");
            }
        };
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new Scheduler(employees, tasks).scheduleTask(new Task(Skill.A, 4)));
        assertEquals("storage failed", error.getMessage());
        Employee employee = employees.getEmployees().get(0);
        assertEquals(0, employee.getLoad());
        assertSame(employee, employees.getEmployeeWithLowestLoad());
        assertTrue(tasks.getTasksByEmployee(employee).isEmpty());
    }

    @Test
    void alreadyAssignedTaskIsRejectedBeforeCallingEitherManager() throws Exception {
        Employee employee = new Employee("employee", Set.of(Skill.A));
        Task task = new Task(Skill.A, 4);
        task.assignTask(employee);
        EmployeeManager employees = new EmployeeManager() {
            @Override
            public Employee assignEmployee(Task task) {
                return fail("Assigned task must not reach employee assignment");
            }

            @Override
            public Employee rollbackAssignment(Employee selected, Task task) {
                return fail("No new assignment was made");
            }
        };
        TaskManager tasks = new TaskManager() {
            @Override
            public void assignTask(Task task, Employee selected) {
                fail("Assigned task must not reach task recording");
            }
        };

        ScheduleException error = assertThrows(ScheduleException.class,
                () -> new Scheduler(employees, tasks).scheduleTask(task));

        assertEquals("Task is already assigned", error.getMessage());
        assertSame(employee, task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.ASSIGNED, task.getStatus());
        assertEquals(0, employee.getLoad());
    }

    @Test
    void concurrentSchedulingOfSameTaskAssignsAndRecordsExactlyOnce() throws Exception {
        AtomicInteger assignmentCalls = new AtomicInteger();
        AtomicInteger recordingCalls = new AtomicInteger();
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();
        Employee employee = new Employee("employee", Set.of(Skill.A));
        EmployeeManager employees = new EmployeeManager() {
            @Override
            public Employee assignEmployee(Task task) throws ScheduleException {
                assertTrue(Thread.holdsLock(task), "Assignment must hold the task monitor");
                assignmentCalls.incrementAndGet();
                return super.assignEmployee(task);
            }
        };
        employees.addEmployee(employee);
        TaskManager tasks = new TaskManager() {
            @Override
            public void assignTask(Task task, Employee selected) throws ScheduleException {
                assertTrue(Thread.holdsLock(task), "Recording must hold the task monitor");
                recordingCalls.incrementAndGet();
                super.assignTask(task, selected);
            }
        };
        Task task = new Task(Skill.A, 4);
        // Separate schedulers must coordinate through the shared task as well.
        support.ConcurrentTestSupport.runWorkers(8, worker -> {
            try {
                new Scheduler(employees, tasks).scheduleTask(task);
                successes.incrementAndGet();
            } catch (ScheduleException error) {
                assertEquals("Task is already assigned", error.getMessage());
                rejections.incrementAndGet();
            }
        });

        assertEquals(1, successes.get());
        assertEquals(7, rejections.get());
        assertEquals(1, assignmentCalls.get());
        assertEquals(1, recordingCalls.get());
        assertEquals(4, employee.getLoad());
        assertSame(employee, task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.ASSIGNED, task.getStatus());
        assertEquals(java.util.List.of(task), tasks.getTasksByEmployee(employee));
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

