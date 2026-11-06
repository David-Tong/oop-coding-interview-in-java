package employee;

import scheduling.ScheduleException;
import task.Task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EmployeeManager {
    private final List<Employee> employees;

    public EmployeeManager() {
        employees = new ArrayList<>();
    }

    public synchronized void addEmployee(Employee employee) {
        employees.add(employee);
    }

    public synchronized Employee assignEmployee(Task task) throws ScheduleException {
        if (task.getStatus() != Task.TaskStatus.PENDING) {
            throw new ScheduleException("Task is not in a pending state and cannot be assigned.");
        }
        Employee employee = employees.stream()
                .filter(e -> e.getSkillSet().contains(task.getSkill()))
                .min(Comparator.comparingInt(Employee::getLoad))
                .orElse(null);
        if (employee == null) {
            return null;
        }
        employee.addLoad(task.getLoad());
        return employee;
    }

    public synchronized Employee rollbackAssignment(Employee employee, Task task) {
        employee.removeLoad(task.getLoad());
        return employee;
    }

    public Employee getEmployeeWithHighestLoad() {
        return employees.stream().max(Comparator.comparingInt(Employee::getLoad))
                .orElse(null);
    }

    public Employee getEmployeeWithLowestLoad() {
        return employees.stream().min(Comparator.comparingInt(Employee::getLoad))
                .orElse(null);
    }

    public List<Employee> getEmployees() {
        return List.copyOf(employees);
    }
}
