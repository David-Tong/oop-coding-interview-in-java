package employee;

import org.junit.jupiter.api.RepeatedTest;
import task.Task;
import scheduling.ScheduleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static support.ConcurrentTestSupport.runWorkers;

@Timeout(30)
class EmployeeManagerConcurrencyTest {
    @RepeatedTest(3)
    void concurrentRegistrationPreservesEveryEmployeeAndEverySkillIndex() throws Exception {
        EmployeeManager manager = new EmployeeManager();
        int workers = 8;
        int perWorker = 40;
        runWorkers(workers, worker -> {
            for (int i = 0; i < perWorker; i++) {
                Employee employee = new Employee(worker + "-" + i, Set.of(Skill.values()));
                employee.addLoad(worker * perWorker + i);
                manager.addEmployee(employee);
            }
        });

        List<Employee> registered = manager.getEmployees();
        assertEquals(workers * perWorker, registered.size());
        assertEquals(registered.size(), new HashSet<>(registered).size());
        assertEquals(registered.size(), registered.stream().map(Employee::getId).distinct().count());
        // Walk each queue by increasing its selected employee's load. This detects lost
        // entries and duplicate queue entries, which repeated peek assertions cannot detect.
        for (Skill skill : Skill.values()) {
            assertQueueContainsExactly(manager, skill, registered);
        }
    }

    @RepeatedTest(3)
    void simultaneousAssignmentAndRollbackHaveExactNetWorkload() throws Exception {
        EmployeeManager manager = new EmployeeManager();
        Employee shared = new Employee("shared", Set.of(Skill.values()));
        manager.addEmployee(shared);
        manager.assignEmployee(new Task(Skill.A, 100_000));
        int workers = 12;
        int iterations = 500;
        runWorkers(workers, worker -> {
            for (int i = 0; i < iterations; i++) {
                if (worker % 2 == 0) {
                    manager.assignEmployee(new Task(Skill.A, 7));
                } else {
                    manager.rollbackAssignment(shared, new Task(Skill.A, 3));
                }
            }
        });
        assertEquals(100_000 + (workers / 2) * iterations * (7 - 3), shared.getLoad());
        assertSame(shared, manager.getEmployeeWithHighestLoad());
        assertSame(shared, manager.getEmployeeWithLowestLoad());
        for (Skill skill : Skill.values()) {
            assertSame(shared, manager.assignEmployee(new Task(skill, 0)));
        }
    }

    @RepeatedTest(3)
    void concurrentAssignmentsChooseMinimumAndKeepEverySkillQueueOrdered() throws Exception {
        EmployeeManager manager = new EmployeeManager();
        List<Employee> team = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Employee employee = new Employee("employee-" + i, Set.of(Skill.values()));
            team.add(employee);
            manager.addEmployee(employee);
        }
        runWorkers(8, worker -> {
            for (int round = 0; round < 400; round++) {
                manager.assignEmployee(new Task(Skill.values()[(worker + round) % 3], 1));
            }
        });
        // With unit loads and atomic minimum selection every employee ends equally loaded.
        team.forEach(employee -> assertEquals(400, employee.getLoad()));
        for (Skill skill : Skill.values()) {
            assertQueueContainsExactly(manager, skill, team);
        }
    }

    @Test
    void readersAndWritersPreserveEligibilityMembershipAndFinalAccounting() throws Exception {
        EmployeeManager manager = new EmployeeManager();
        List<Employee> team = List.of(
                new Employee("ab", Set.of(Skill.A, Skill.B)),
                new Employee("bc", Set.of(Skill.B, Skill.C)),
                new Employee("ac", Set.of(Skill.A, Skill.C)),
                new Employee("abc", Set.of(Skill.values())));
        team.forEach(manager::addEmployee);
        List<Employee> snapshot = manager.getEmployees();
        Set<Employee> members = Set.copyOf(team);
        CyclicBarrier roundStart = new CyclicBarrier(8);
        runWorkers(8, worker -> {
            for (int round = 0; round < 200; round++) {
                roundStart.await(5, TimeUnit.SECONDS);
                if (worker < team.size()) {
                    Employee selected = manager.assignEmployee(new Task(Skill.values()[worker % 3], 3));
                    manager.rollbackAssignment(selected, new Task(Skill.A, 1));
                } else {
                    for (Skill skill : Skill.values()) {
                        Employee selected = manager.assignEmployee(new Task(skill, 0));
                        assertNotNull(selected);
                        assertTrue(members.contains(selected));
                        assertTrue(selected.getSkillSet().contains(skill));
                    }
                    // Workloads may change during the reads, so no minimum is asserted here.
                    assertTrue(members.contains(manager.getEmployeeWithHighestLoad()));
                    assertTrue(members.contains(manager.getEmployeeWithLowestLoad()));
                    assertEquals(members, Set.copyOf(manager.getEmployees()));
                }
            }
        });
        assertEquals(1600, team.stream().mapToInt(Employee::getLoad).sum());
        assertEquals(team, snapshot);
        for (Skill skill : Skill.values()) {
            assertQueueContainsExactly(manager, skill,
                    team.stream().filter(employee -> employee.getSkillSet().contains(skill)).toList());
        }
    }

    @Test
    void independentSkillUpdatesDoNotAffectOtherEmployees() throws Exception {
        EmployeeManager manager = new EmployeeManager();
        List<Employee> team = new ArrayList<>();
        for (Skill skill : Skill.values()) {
            Employee employee = new Employee(skill.name(), Set.of(skill));
            team.add(employee);
            manager.addEmployee(employee);
        }
        runWorkers(9, worker -> {
            Employee employee = team.get(worker % team.size());
            for (int i = 0; i < 300; i++) {
                assertSame(employee, manager.assignEmployee(new Task(Skill.values()[worker % 3], 5)));
                manager.rollbackAssignment(employee, new Task(Skill.A, 2));
                assertSame(employee, manager.assignEmployee(new Task(Skill.values()[worker % 3], 0)));
            }
        });
        team.forEach(employee -> assertEquals(2700, employee.getLoad()));
        assertEquals(3, manager.getEmployees().size());
        EmployeeManager empty = new EmployeeManager();
        assertNull(empty.assignEmployee(new Task(Skill.A, 1)));
    }

    private static void assertQueueContainsExactly(EmployeeManager manager, Skill skill,
                                                   List<Employee> eligible) throws ScheduleException {
        Set<Employee> remaining = new HashSet<>(eligible);
        int penalty = eligible.stream().mapToInt(Employee::getLoad).max().orElse(0) + 1;
        for (int i = 0; i < eligible.size(); i++) {
            int minimum = remaining.stream().mapToInt(Employee::getLoad).min().orElseThrow();
            Employee selected = manager.assignEmployee(new Task(skill, penalty));
            assertNotNull(selected);
            assertTrue(remaining.contains(selected), "Queue selected a duplicate or ineligible employee");
            assertEquals(minimum + penalty, selected.getLoad(), "Queue must expose the current minimum");
            remaining.remove(selected);
        }
        assertTrue(remaining.isEmpty(), "Every eligible employee must remain reachable");
        eligible.forEach(employee -> manager.rollbackAssignment(employee, new Task(Skill.A, penalty)));
    }

}

