package entities;

import managers.*;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ReviewInvariantsTest {
    @Test
    void successfulSchedulingUpdatesTaskMetadata() throws Exception {
        EmployeeManager employees = new EmployeeManager();
        Employee employee = new Employee("e", Set.of(Skill.A));
        employees.addEmployee(employee);
        Task task = new Task(Skill.A, 3);
        new Scheduler(employees, new TaskManager()).scheduleTask(task);
        assertSame(employee, task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.ASSIGNED, task.getStatus());
    }

    @Test
    void recordingFailureDoesNotLeaveUnrecordedWorkload() {
        EmployeeManager employees = new EmployeeManager();
        Employee employee = new Employee("e", Set.of(Skill.A));
        employees.addEmployee(employee);
        TaskManager broken = new TaskManager() {
            @Override public void createTask(Task task, Employee assignee) {
                throw new IllegalStateException("storage failed");
            }
        };
        assertThrows(ScheduleException.class, () -> new Scheduler(employees, broken)
                .scheduleTask(new Task(Skill.A, 3)));
        assertEquals(0, employee.getLoad().get());
    }

    @Test
    void assignmentFailureDoesNotPermanentlyRemoveEmployee() {
        EmployeeManager employees = new EmployeeManager() {
            @Override public void assignemployee(Employee employee, int load) {
                throw new IllegalStateException("assignment failed");
            }
        };
        Employee employee = new Employee("e", Set.of(Skill.A));
        employees.addEmployee(employee);
        assertThrows(ScheduleException.class, () -> new Scheduler(employees, new TaskManager())
                .scheduleTask(new Task(Skill.A, 3)));
        assertSame(employee, employees.requestEmployee("A"));
    }

    @Test
    void anotherSkillQueueMustNotChooseAnEmployeeWhoseLoadHasIncreased() throws Exception {
        CountDownLatch updated = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        Employee shared = new Employee("shared", Set.of(Skill.A, Skill.B)) {
            @Override public Set<Skill> getSkillSet() {
                // assignemployee updates load before requesting skills to refresh queues.
                if (getLoad().get() == 10) {
                    updated.countDown();
                    try {
                        if (!resume.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                }
                return super.getSkillSet();
            }
        };
        Employee lowerLoad = new Employee("lower", Set.of(Skill.B));
        lowerLoad.getLoad().set(5);
        EmployeeManager employees = new EmployeeManager();
        employees.addEmployee(shared);
        employees.addEmployee(lowerLoad);
        assertSame(shared, employees.requestEmployee("A"));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> employees.assignemployee(shared, 10));
        try {
            assertTrue(updated.await(5, TimeUnit.SECONDS));
            assertEquals(10, shared.getLoad().get());
            assertSame(lowerLoad, employees.requestEmployee("B"));
        } finally {
            resume.countDown();
            future.get(5, TimeUnit.SECONDS);
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
