package entities;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class TaskTest {
    @Test
    void retainsTaskDetailsAndAllowsEmployeeReassignment() {
        Task task = new Task(Skill.C, 9);
        assertNotNull(UUID.fromString(task.getId()));
        assertNotEquals(task.getId(), new Task(Skill.C, 9).getId());
        assertEquals(Skill.C, task.getSkill());
        assertEquals(9, task.getLoad());
        assertNull(task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.PENDING, task.getStatus());
        Employee first = new Employee("first", Set.of(Skill.C));
        Employee second = new Employee("second", Set.of(Skill.C));
        task.setEmployee(first);
        assertSame(first, task.getAssignedEmployee());
        task.setEmployee(second);
        assertSame(second, task.getAssignedEmployee());
    }

    @Test
    void updatesAssignedEmployeeAndStatus() {
        Employee employee = new Employee("employee", Set.of(Skill.B));
        Task task = new Task(Skill.B, 1);
        task.setEmployee(employee);
        task.setTaskStatus(Task.TaskStatus.ASSIGNED);
        assertSame(employee, task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.ASSIGNED, task.getStatus());
    }
}
