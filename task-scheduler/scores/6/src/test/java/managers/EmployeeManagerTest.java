package managers;

import entities.Employee;
import entities.Skill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import static support.ConcurrentTestSupport.runWorkers;

@Timeout(20)
class EmployeeManagerTest {
    @Test
    void emptyManagerReturnsEmptyResults() {
        EmployeeManager manager = new EmployeeManager();
        assertTrue(manager.getEmployees().isEmpty());
        assertNull(manager.requestEmployee("A"));
        assertNull(manager.getEmployeeWithHighestLoad());
        assertNull(manager.getEmployeeWithLowestLoad());
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
    void assignmentReordersEverySkillQueueAndRestoresCheckedOutEmployee() {
        EmployeeManager manager = new EmployeeManager();
        Employee first = new Employee("first", Set.of(Skill.A, Skill.B));
        Employee second = new Employee("second", Set.of(Skill.A, Skill.B));
        second.getLoad().set(3);
        manager.addEmployee(first);
        manager.addEmployee(second);
        assertSame(first, manager.requestEmployee("A"));
        manager.assignemployee(first, 7);
        assertEquals(7, first.getLoad().get());
        assertSame(second, manager.requestEmployee("A"));
        assertSame(second, manager.requestEmployee("B"));
        assertSame(first, manager.requestEmployee("A"));
        assertSame(first, manager.requestEmployee("B"));
        assertNull(manager.requestEmployee("A"));
        assertSame(first, manager.getEmployeeWithHighestLoad());
        assertSame(second, manager.getEmployeeWithLowestLoad());
    }

    @Test
    void concurrentRequestsForSameSkillCheckOutEachEmployeeOnce() throws Exception {
        EmployeeManager manager = new EmployeeManager();
        Set<Employee> expected = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < 80; i++) {
            Employee employee = new Employee("e" + i, Set.of(Skill.A));
            expected.add(employee);
            manager.addEmployee(employee);
        }
        Set<Employee> checkedOut = ConcurrentHashMap.newKeySet();
        runWorkers(8, worker -> {
            for (int i = 0; i < 10; i++) {
                Employee employee = manager.requestEmployee("A");
                assertNotNull(employee);
                assertTrue(checkedOut.add(employee), "Employee checked out twice");
            }
        });
        assertEquals(expected, checkedOut);
        assertNull(manager.requestEmployee("A"));
    }

    @Test
    void concurrentAssignmentsAccumulateLoadWithoutDuplicatingQueueEntries() throws Exception {
        EmployeeManager manager = new EmployeeManager();
        Employee employee = new Employee("shared", Set.of(Skill.A, Skill.B));
        manager.addEmployee(employee);
        runWorkers(8, worker -> {
            for (int i = 0; i < 200; i++) manager.assignemployee(employee, 2);
        });
        assertEquals(3200, employee.getLoad().get());
        for (Skill skill : employee.getSkillSet()) {
            assertSame(employee, manager.requestEmployee(skill.name()));
            assertNull(manager.requestEmployee(skill.name()));
        }
    }
}
