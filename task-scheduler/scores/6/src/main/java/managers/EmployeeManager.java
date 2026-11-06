package managers;

import entities.Employee;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class EmployeeManager {
    private final List<Employee> employees;
    private final ConcurrentHashMap<String, PriorityQueue<Employee>> employeesBySkiils;

    public EmployeeManager() {
        employees = new ArrayList<>();
        employeesBySkiils = new ConcurrentHashMap<>();
    }

    public void addEmployee(Employee employee) {
        employees.add(employee);
        employee.getSkillSet().forEach(skill -> {
            employeesBySkiils.putIfAbsent(skill.name(), new PriorityQueue<>());
            employeesBySkiils.get(skill.name()).offer(employee);
        });
    }

    public Employee requestEmployee(String skill) {
        AtomicReference<Employee> employee = new AtomicReference<>();
        employeesBySkiils.computeIfPresent(skill, (k, v) -> {
            employee.set(v.poll());
            return v;
        });
        return employee.get();
    }

    public void assignemployee(Employee employee, int workload) {
        employee.getLoad().addAndGet(workload);
        employee.getSkillSet().forEach(skill -> {
            employeesBySkiils.computeIfPresent(skill.name(), (k, v) -> {
                v.remove(employee);
                v.add(employee);
                return v;
            });
        });
    }

    public Employee getEmployeeWithHighestLoad() {
        return employees.stream().max((e1, e2) -> Integer.compare(e1.getLoad().get(), e2.getLoad().get()))
                .orElse(null);
    }

    public Employee getEmployeeWithLowestLoad() {
        return employees.stream().min((e1, e2) -> Integer.compare(e1.getLoad().get(), e2.getLoad().get()))
                .orElse(null);
    }

    public List<Employee> getEmployees() {
        return List.copyOf(employees);
    }
}
