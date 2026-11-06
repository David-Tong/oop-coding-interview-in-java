package managers;

import entities.Employee;
import entities.Skill;
import entities.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TaskManager {
    private final ConcurrentHashMap<Employee, List<Task>> tasksByEmployee;

    public TaskManager() {
        tasksByEmployee = new ConcurrentHashMap<>();
    }

    public void createTask(Task task, Employee assignedEmployee) {
        tasksByEmployee.compute(assignedEmployee, (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            task.setEmployee(assignedEmployee);
            task.setTaskStatus(Task.TaskStatus.ASSIGNED);
            v.add(task);
            return v;
        });
    }

    public List<Task> getTasksByEmployee(Employee employee) {
        return List.copyOf(tasksByEmployee.getOrDefault(employee, new ArrayList<>()));
    }
}
