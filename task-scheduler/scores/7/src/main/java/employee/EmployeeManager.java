package employee;

import scheduling.ScheduleException;
import task.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EmployeeManager {
    private final List<Employee> employees;
    private final ConcurrentHashMap<String, PriorityQueue<Employee>> employeesBySkiils;
    private final Lock lock;

    public EmployeeManager() {
        employees = new ArrayList<>();
        employeesBySkiils = new ConcurrentHashMap<>();
        lock = new ReentrantLock();
    }

    public void addEmployee(Employee employee) {
        lock.lock();
        try {
            employees.add(employee);
            employee.getSkillSet().forEach(skill -> {
                employeesBySkiils.putIfAbsent(skill.name(), new PriorityQueue<>());
                employeesBySkiils.get(skill.name()).offer(employee);
            });
        } finally {
            lock.unlock();
        }
    }

    public Employee assignEmployee(Task task) throws ScheduleException {
        if (task.getStatus() != Task.TaskStatus.PENDING) {
            throw new ScheduleException("Task is not in a pending state and cannot be assigned.");
        }
        try {
            lock.lock();
            PriorityQueue<Employee> eligible = employeesBySkiils.get(task.getSkill().name());
            if (eligible == null || eligible.isEmpty()) {
                return null;
            }
            Employee employee = eligible.peek();
            employee.addLoad(task.getLoad());
            try {
                reorder(employee);
            } catch (Exception e) {
                employee.removeLoad(task.getLoad());
                reorder(employee);
                throw new ScheduleException("Failed to assign employee for task with skill " + task.getSkill() + " and load " + task.getLoad());
            }
            return employee;
        } finally {
            lock.unlock();
        }
    }

    public void rollbackAssignment(Employee employee, int workload) {
        lock.lock();
        try {
            employee.removeLoad(workload);
            reorder(employee);
        } finally {
            lock.unlock();
        }
    }

    private void reorder(Employee employee) {
        employee.getSkillSet().forEach(skill -> {
            PriorityQueue<Employee> queue = employeesBySkiils.get(skill.name());
            queue.remove(employee);
            queue.offer(employee);
        });
    }

    public Employee getEmployeeWithHighestLoad() {
        lock.lock();
        try {
            return employees.stream().max((e1, e2) -> Integer.compare(e1.getLoad(), e2.getLoad()))
                    .orElse(null);
        } finally {
            lock.unlock();
        }
    }

    public Employee getEmployeeWithLowestLoad() {
        lock.lock();
        try {
            return employees.stream().min((e1, e2) -> Integer.compare(e1.getLoad(), e2.getLoad()))
                    .orElse(null);
        } finally {
            lock.unlock();
        }
    }

    public List<Employee> getEmployees() {
        lock.lock();
        try {
            return List.copyOf(employees);
        } finally {
            lock.unlock();
        }
    }
}
