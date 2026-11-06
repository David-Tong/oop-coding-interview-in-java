package task;
import employee.Employee;
import employee.Skill;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import scheduling.ScheduleException;
import static org.junit.jupiter.api.Assertions.*;

class TaskTest {
    @Test
    void retainsTaskDetailsAndRejectsEmployeeReassignment() throws Exception {
        Task task = new Task(Skill.C, 9);
        assertNotNull(UUID.fromString(task.getId()));
        assertNotEquals(task.getId(), new Task(Skill.C, 9).getId());
        assertEquals(Skill.C, task.getSkill());
        assertEquals(9, task.getLoad());
        assertNull(task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.PENDING, task.getStatus());
        Employee first = new Employee("first", Set.of(Skill.C));
        Employee second = new Employee("second", Set.of(Skill.C));
        task.assignTask(first);
        assertSame(first, task.getAssignedEmployee());
        assertThrows(ScheduleException.class, () -> task.assignTask(second));
        assertSame(first, task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.ASSIGNED, task.getStatus());
    }

    @Test
    void updatesAssignedEmployeeAndStatus() throws Exception {
        Employee employee = new Employee("employee", Set.of(Skill.B));
        Task task = new Task(Skill.B, 1);
        task.assignTask(employee);
        assertSame(employee, task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.ASSIGNED, task.getStatus());
        assertThrows(ScheduleException.class, () -> task.assignTask(employee));
        assertSame(employee, task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.ASSIGNED, task.getStatus());
    }

    @Test
    void rejectsEmployeeAssignmentAfterLeavingPendingState() throws Exception {
        Task task = new Task(Skill.A, 1);
        // With a null owner, the status guard independently prevents a second assignment.
        task.assignTask(null);
        assertThrows(ScheduleException.class,
                () -> task.assignTask(new Employee("late", Set.of(Skill.A))));
        assertNull(task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.ASSIGNED, task.getStatus());
    }
}
