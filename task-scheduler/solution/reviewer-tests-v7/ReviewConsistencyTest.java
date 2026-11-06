package employee;

import org.junit.jupiter.api.Test;
import scheduling.ScheduleException;
import scheduling.Scheduler;
import task.Task;
import task.TaskManager;
import java.util.Set;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ReviewConsistencyTest {
    @Test
    void readerMustNotObserveStaleSkillQueueDuringRefresh() throws Exception {
        CountDownLatch changed = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        Employee shared = new Employee("shared", Set.of(Skill.A, Skill.B)) {
            @Override Set<Skill> getSkillSet() {
                if (getLoad() == 10) {
                    changed.countDown();
                    await(resume);
                }
                return super.getSkillSet();
            }
        };
        Employee lower = new Employee("lower", Set.of(Skill.B));
        lower.setLoad(5);
        EmployeeManager manager = new EmployeeManager();
        manager.addEmployee(shared);
        manager.addEmployee(lower);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<?> update = pool.submit(() -> manager.assignEmployee(shared, 10));
        try {
            assertTrue(changed.await(5, TimeUnit.SECONDS));
            assertSame(lower, manager.requestEmployee("B"));
        } finally {
            resume.countDown();
            update.get(5, TimeUnit.SECONDS);
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void selectionAndAssignmentMustNotUseSameStaleMinimum() throws Exception {
        CountDownLatch selected = new CountDownLatch(2);
        EmployeeManager manager = new EmployeeManager() {
            @Override public void assignEmployee(Employee employee, int load) {
                selected.countDown();
                await(selected);
                super.assignEmployee(employee, load);
            }
        };
        Employee first = new Employee("first", Set.of(Skill.A));
        Employee second = new Employee("second", Set.of(Skill.A));
        second.setLoad(1);
        manager.addEmployee(first);
        manager.addEmployee(second);
        Scheduler scheduler = new Scheduler(manager, new TaskManager());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> a = pool.submit(() -> { scheduler.scheduleTask(new Task(Skill.A, 10)); return null; });
            Future<?> b = pool.submit(() -> { scheduler.scheduleTask(new Task(Skill.A, 10)); return null; });
            a.get(5, TimeUnit.SECONDS);
            b.get(5, TimeUnit.SECONDS);
            assertEquals(10, first.getLoad());
            assertEquals(11, second.getLoad());
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void failureBeforeAssignmentMustNotSubtractUnaddedLoad() {
        EmployeeManager manager = new EmployeeManager() {
            @Override public void assignEmployee(Employee employee, int load) {
                throw new IllegalStateException("before update");
            }
        };
        Employee employee = new Employee("e", Set.of(Skill.A));
        manager.addEmployee(employee);
        assertThrows(ScheduleException.class, () -> new Scheduler(manager, new TaskManager())
                .scheduleTask(new Task(Skill.A, 3)));
        assertEquals(0, employee.getLoad());
    }

    @Test
    void failureAfterRecordingMustNotLeaveRecordWithoutLoad() {
        EmployeeManager manager = new EmployeeManager();
        Employee employee = new Employee("e", Set.of(Skill.A));
        manager.addEmployee(employee);
        TaskManager tasks = new TaskManager() {
            @Override public void createTask(Task task, Employee employee) {
                super.createTask(task, employee);
                throw new IllegalStateException("after record");
            }
        };
        Task task = new Task(Skill.A, 3);
        assertThrows(ScheduleException.class, () -> new Scheduler(manager, tasks).scheduleTask(task));
        assertEquals(tasks.getTasksByEmployee(employee).stream().mapToInt(Task::getLoad).sum(),
                employee.getLoad());
    }

    @Test
    void failureBeforeRecordingRestoresLoad() {
        EmployeeManager manager = new EmployeeManager();
        Employee employee = new Employee("e", Set.of(Skill.A));
        manager.addEmployee(employee);
        TaskManager tasks = new TaskManager() {
            @Override public void createTask(Task task, Employee employee) {
                throw new IllegalStateException("before record");
            }
        };
        assertThrows(ScheduleException.class, () -> new Scheduler(manager, tasks)
                .scheduleTask(new Task(Skill.A, 3)));
        assertEquals(0, employee.getLoad());
        assertSame(employee, manager.requestEmployee("A"));
    }

    @Test
    void sequentialDuplicateSubmissionPreservesOriginalAssignment() throws Exception {
        EmployeeManager manager = new EmployeeManager();
        Employee employee = new Employee("e", Set.of(Skill.A));
        manager.addEmployee(employee);
        TaskManager tasks = new TaskManager();
        Scheduler scheduler = new Scheduler(manager, tasks);
        Task task = new Task(Skill.A, 3);
        scheduler.scheduleTask(task);
        assertThrows(ScheduleException.class, () -> scheduler.scheduleTask(task));
        assertEquals(3, employee.getLoad());
        assertEquals(1, tasks.getTasksByEmployee(employee).size());
        assertSame(employee, task.getAssignedEmployee());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
