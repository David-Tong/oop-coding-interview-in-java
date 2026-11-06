package employee;

import org.junit.jupiter.api.Test;
import task.Task;
import scheduling.ScheduleException;
import org.junit.jupiter.api.Timeout;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import static support.ConcurrentTestSupport.runWorkers;

@Timeout(20)
class EmployeeManagerTest {
    @Test
    void emptyManagerReturnsEmptyResults() throws Exception {
        EmployeeManager manager = new EmployeeManager();
        assertTrue(manager.getEmployees().isEmpty());
        assertNull(manager.assignEmployee(new Task(Skill.A, 0)));
        assertNull(manager.getEmployeeWithHighestLoad());
        assertNull(manager.getEmployeeWithLowestLoad());
    }

    @Test
    void rejectsAlreadyAssignedTaskWithoutChangingLoad() throws Exception {
        EmployeeManager manager = new EmployeeManager();
        Employee employee = new Employee("only", Set.of(Skill.A));
        manager.addEmployee(employee);
        Task task = new Task(Skill.A, 5);
        task.assignTask(employee);
        assertThrows(ScheduleException.class, () -> manager.assignEmployee(task));
        assertEquals(0, employee.getLoad());
    }

    @Test
    void employeeListIsAnImmutableSnapshot() {
        EmployeeManager manager = new EmployeeManager();
        Employee first = new Employee("first", Set.of(Skill.A));
        manager.addEmployee(first);
        List<Employee> snapshot = manager.getEmployees();
        manager.addEmployee(new Employee("second", Set.of(Skill.B)));
        assertEquals(List.of(first), snapshot);
        assertEquals(2, manager.getEmployees().size());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
    }

    @Test
    void assignmentAndRollbackUpdateSelectionForEverySkill() throws Exception {
        EmployeeManager manager = new EmployeeManager();
        Employee first = new Employee("first", Set.of(Skill.A, Skill.B));
        Employee second = new Employee("second", Set.of(Skill.A, Skill.B));
        second.addLoad(3);
        manager.addEmployee(first);
        manager.addEmployee(second);
        assertSame(first, manager.assignEmployee(new Task(Skill.A, 0)));
        assertSame(first, manager.assignEmployee(new Task(Skill.A, 7)));
        assertEquals(7, first.getLoad());
        assertSame(second, manager.assignEmployee(new Task(Skill.A, 0)));
        assertSame(second, manager.assignEmployee(new Task(Skill.B, 0)));
        assertSame(second, manager.assignEmployee(new Task(Skill.A, 0)));
        assertSame(second, manager.assignEmployee(new Task(Skill.B, 0)));
        assertSame(first, manager.getEmployeeWithHighestLoad());
        assertSame(second, manager.getEmployeeWithLowestLoad());
        manager.rollbackAssignment(first, new Task(Skill.A, 7));
        assertEquals(0, first.getLoad());
        assertSame(first, manager.assignEmployee(new Task(Skill.A, 0)));
        assertSame(first, manager.assignEmployee(new Task(Skill.B, 0)));
    }

    @Test
    void concurrentZeroLoadAssignmentsDoNotRemoveLowestLoadEmployee() throws Exception {
        EmployeeManager manager = new EmployeeManager();
        Set<Employee> expected = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < 80; i++) {
            Employee employee = new Employee("e" + i, Set.of(Skill.A));
            expected.add(employee);
            employee.addLoad(i);
            manager.addEmployee(employee);
        }
        Set<Employee> checkedOut = ConcurrentHashMap.newKeySet();
        runWorkers(8, worker -> {
            for (int i = 0; i < 10; i++) {
                Employee employee = manager.assignEmployee(new Task(Skill.A, 0));
                assertNotNull(employee);
                assertEquals("e0", employee.getId());
                checkedOut.add(employee);
            }
        });
        assertEquals(1, checkedOut.size());
        assertEquals(expected, Set.copyOf(manager.getEmployees()));
        assertTrue(checkedOut.contains(manager.assignEmployee(new Task(Skill.A, 0))));
    }

    @Test
    void concurrentAssignmentsAccumulateLoadAndKeepEmployeeAvailable() throws Exception {
        EmployeeManager manager = new EmployeeManager();
        Employee employee = new Employee("shared", Set.of(Skill.A, Skill.B));
        manager.addEmployee(employee);
        runWorkers(8, worker -> {
            for (int i = 0; i < 200; i++) assertSame(employee, manager.assignEmployee(new Task(Skill.A, 2)));
        });
        assertEquals(3200, employee.getLoad());
        for (Skill skill : employee.getSkillSet()) {
            assertSame(employee, manager.assignEmployee(new Task(skill, 0)));
            assertSame(employee, manager.assignEmployee(new Task(skill, 0)));
        }
    }
}
