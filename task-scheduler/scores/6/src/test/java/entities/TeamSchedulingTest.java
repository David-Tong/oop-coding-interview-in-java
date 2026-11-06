package entities;

import managers.EmployeeManager;
import managers.ScheduleException;
import managers.Scheduler;
import managers.TaskManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class TeamSchedulingTest {
    private final EmployeeManager employees = new EmployeeManager();
    private final TaskManager tasks = new TaskManager();
    private final Scheduler scheduler = new Scheduler(employees, tasks);

    private static Task task(Skill skill, int load) {
        return new Task(skill, load);
    }

    @Test
    void schedulesOnlyToEmployeesWithRequiredSkill() throws Exception {
        Employee matching = new Employee("matching", Set.of(Skill.A));
        Employee other = new Employee("other", Set.of(Skill.B));
        employees.addEmployee(matching);
        employees.addEmployee(other);
        Task task = task(Skill.A, 7);
        scheduler.scheduleTask(task);
        assertEquals(List.of(task), tasks.getTasksByEmployee(matching));
        assertEquals(7, matching.getLoad().get());
        assertTrue(tasks.getTasksByEmployee(other).isEmpty());
        assertEquals(0, other.getLoad().get());
    }

    @Test
    void choosesLowestLoadAndReusesEmployeeAfterAssignment() throws Exception {
        Employee busy = new Employee("busy", Set.of(Skill.A));
        Employee available = new Employee("available", Set.of(Skill.A));
        busy.getLoad().set(10);
        employees.addEmployee(busy);
        employees.addEmployee(available);
        Task first = task(Skill.A, 3);
        Task second = task(Skill.A, 4);
        scheduler.scheduleTask(first);
        scheduler.scheduleTask(second);
        assertEquals(List.of(first, second), tasks.getTasksByEmployee(available));
        assertEquals(7, available.getLoad().get());
        assertSame(busy, employees.getEmployeeWithHighestLoad());
        assertSame(available, employees.getEmployeeWithLowestLoad());
    }

    @Test
    void rejectsUnsupportedSkillWithoutChangingLoad() {
        Employee employee = new Employee("employee", Set.of(Skill.A));
        employees.addEmployee(employee);
        ScheduleException error = assertThrows(ScheduleException.class,
                () -> scheduler.scheduleTask(task(Skill.C, 5)));
        assertTrue(error.getMessage().contains("No available employee"));
        assertEquals(0, employee.getLoad().get());
        assertTrue(tasks.getTasksByEmployee(employee).isEmpty());
    }

    @Test
    void emptyTeamHasNoLoadExtremesAndCannotSchedule() {
        assertNull(employees.getEmployeeWithHighestLoad());
        assertNull(employees.getEmployeeWithLowestLoad());
        assertThrows(ScheduleException.class,
                () -> scheduler.scheduleTask(task(Skill.A, 1)));
    }

    @Test
    void concurrentSchedulingRecordsEverySuccessfulTaskExactlyOnce() throws Exception {
        // Same team and 5,000 attempts as Team.main, with reproducible task inputs.
        employees.addEmployee(new Employee("1", Set.of(Skill.A, Skill.B)));
        employees.addEmployee(new Employee("2", Set.of(Skill.B, Skill.C)));
        employees.addEmployee(new Employee("3", Set.of(Skill.A, Skill.C)));
        employees.addEmployee(new Employee("4", Set.of(Skill.A, Skill.B, Skill.C)));
        employees.addEmployee(new Employee("5", Set.of(Skill.B)));
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<Task>>> futures = new ArrayList<>();
        try {
            for (int worker = 0; worker < 10; worker++) {
                int offset = worker * 500;
                futures.add(pool.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    List<Task> successful = new ArrayList<>();
                    for (int index = 0; index < 500; index++) {
                        int id = offset + index;
                        Task task = task(Skill.values()[id % 3], id % 9 + 1);
                        try {
                            scheduler.scheduleTask(task);
                            successful.add(task);
                        } catch (ScheduleException expectedUnderContention) {
                            assertTrue(expectedUnderContention.getMessage().startsWith("No available employee"),
                                    expectedUnderContention.getMessage());
                        }
                    }
                    return successful;
                }));
            }
            start.countDown();
            Set<Task> successful = new HashSet<>();
            for (Future<List<Task>> future : futures) successful.addAll(future.get(15, TimeUnit.SECONDS));
            assertFalse(successful.isEmpty());
            List<Task> recorded = new ArrayList<>();
            for (Employee employee : employees.getEmployees()) {
                List<Task> assigned = tasks.getTasksByEmployee(employee);
                recorded.addAll(assigned);
                assertTrue(assigned.stream().allMatch(t -> employee.getSkillSet().contains(t.getSkill())));
                assertEquals(assigned.stream().mapToInt(Task::getLoad).sum(), employee.getLoad().get());
            }
            assertEquals(successful.size(), recorded.size(), "No duplicate task records");
            assertEquals(successful, new HashSet<>(recorded), "No lost or rejected tasks recorded");
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void overlappingRequestFailsWhileOnlyMatchingEmployeeIsCheckedOut() throws Exception {
        CountDownLatch checkedOut = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        EmployeeManager controlled = new EmployeeManager() {
            @Override
            public void assignemployee(Employee employee, int workload) {
                checkedOut.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for concurrent request");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                super.assignemployee(employee, workload);
            }
        };
        Employee employee = new Employee("only", Set.of(Skill.A));
        controlled.addEmployee(employee);
        Scheduler controlledScheduler = new Scheduler(controlled, tasks);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Task first = task(Skill.A, 3);
        try {
            Future<?> pending = pool.submit(() -> {
                controlledScheduler.scheduleTask(first);
                return null;
            });
            assertTrue(checkedOut.await(5, TimeUnit.SECONDS));
            assertThrows(ScheduleException.class,
                    () -> controlledScheduler.scheduleTask(task(Skill.A, 7)));
            release.countDown();
            pending.get(5, TimeUnit.SECONDS);
            Task later = task(Skill.A, 2);
            controlledScheduler.scheduleTask(later);
            assertEquals(List.of(first, later), tasks.getTasksByEmployee(employee));
            assertEquals(5, employee.getLoad().get());
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
