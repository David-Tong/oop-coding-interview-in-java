package task;

import employee.Employee;
import scheduling.ScheduleException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TaskManager {
    private final ConcurrentHashMap<Employee, List<Task>> tasksByEmployee;

    public TaskManager() {
        tasksByEmployee = new ConcurrentHashMap<>();
    }

    public void assignTask(Task task, Employee employee) throws ScheduleException {
        if (task.getAssignedEmployee() != null) {
            throw new ScheduleException("Task is already assigned to an employee. Reassignment is not allowed.");
        }
        if (task.getStatus() != Task.TaskStatus.PENDING) {
            throw new ScheduleException("Task is not in a pending state and cannot be assigned.");
        }
        tasksByEmployee.compute(employee, (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            try {
                task.assignTask(employee);
            } catch (ScheduleException e) {
                throw new RuntimeException(e);
            }

            v.add(task);
            return v;
        });
    }

    public List<Task> getTasksByEmployee(Employee employee) {
        return List.copyOf(tasksByEmployee.getOrDefault(employee, new ArrayList<>()));
    }
}
