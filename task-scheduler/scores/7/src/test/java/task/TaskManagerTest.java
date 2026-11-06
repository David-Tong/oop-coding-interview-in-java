package task;

import employee.Employee;
import employee.Skill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static support.ConcurrentTestSupport.runWorkers;

@Timeout(20)
class TaskManagerTest {
    @Test
    void groupsTasksByEmployeeInInsertionOrderAndReturnsImmutableSnapshots() throws Exception {
        TaskManager manager = new TaskManager();
        Employee first = new Employee("first", Set.of(Skill.A));
        Employee second = new Employee("second", Set.of(Skill.B));
        assertTrue(manager.getTasksByEmployee(first).isEmpty());
        Task one = new Task(Skill.A, 2);
        Task two = new Task(Skill.A, 3);
        Task three = new Task(Skill.B, 1);
        manager.assignTask(one, first);
        List<Task> snapshot = manager.getTasksByEmployee(first);
        manager.assignTask(two, first);
        manager.assignTask(three, second);
        assertEquals(List.of(one), snapshot);
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertEquals(List.of(one, two), manager.getTasksByEmployee(first));
        assertEquals(List.of(three), manager.getTasksByEmployee(second));
        assertSame(first, one.getAssignedEmployee());
        assertSame(first, two.getAssignedEmployee());
        assertSame(second, three.getAssignedEmployee());
        assertEquals(Task.TaskStatus.ASSIGNED, one.getStatus());
        assertEquals(Task.TaskStatus.ASSIGNED, two.getStatus());
        assertEquals(Task.TaskStatus.ASSIGNED, three.getStatus());
    }

    @Test
    void concurrentWritesToSameAndDifferentEmployeesLoseNoTasks() throws Exception {
        TaskManager manager = new TaskManager();
        Employee shared = new Employee("shared", Set.of(Skill.A));
        Employee other = new Employee("other", Set.of(Skill.A));
        runWorkers(8, worker -> {
            for (int i = 0; i < 250; i++) {
                manager.assignTask(new Task(Skill.A, 1),
                        worker < 6 ? shared : other);
            }
        });
        List<Task> sharedTasks = manager.getTasksByEmployee(shared);
        List<Task> otherTasks = manager.getTasksByEmployee(other);
        assertEquals(1500, sharedTasks.size());
        assertEquals(500, otherTasks.size());
        sharedTasks.forEach(task -> {
            assertSame(shared, task.getAssignedEmployee());
            assertEquals(Task.TaskStatus.ASSIGNED, task.getStatus());
        });
        otherTasks.forEach(task -> {
            assertSame(other, task.getAssignedEmployee());
            assertEquals(Task.TaskStatus.ASSIGNED, task.getStatus());
        });
        Set<String> ids = new HashSet<>();
        sharedTasks.forEach(task -> assertTrue(ids.add(task.getId())));
        otherTasks.forEach(task -> assertTrue(ids.add(task.getId())));
        assertEquals(2000, ids.size());
    }
}

