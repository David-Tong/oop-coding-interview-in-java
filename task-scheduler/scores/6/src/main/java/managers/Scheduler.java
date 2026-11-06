package managers;

import entities.Employee;
import entities.Skill;
import entities.Task;


public class Scheduler {
    private final EmployeeManager employeeManager;
    private final TaskManager taskManager;

    public Scheduler(EmployeeManager employeeManager, TaskManager taskManager) {
        this.employeeManager = employeeManager;
        this.taskManager = taskManager;
    }

    public void scheduleTask(Task task) throws ScheduleException {
        Employee employee = employeeManager.requestEmployee(task.getSkill().name());
        if (employee != null) {
            try {
                employeeManager.assignemployee(employee, task.getLoad());
                taskManager.createTask(task, employee);
            } catch (Exception e) {
                throw new ScheduleException("Failed to assign employee for task with skill " + task.getSkill() + " and load " + task.getLoad());
            }
        } else {
            throw new ScheduleException("No available employee for task with skill " + task.getSkill() + " and load " + task.getLoad());
        }
    }

    public void report() {
        System.out.println("Employee Load Report:");
        for (Employee employee : employeeManager.getEmployees()) {
            System.out.println("Employee ID: " + employee.getId() + ", Load: " + employee.getLoad().get());
            System.out.println("Assigned Tasks:");
            for (Task task : taskManager.getTasksByEmployee(employee)) {
                System.out.println("Task ID: " + task.getId() + ", Skill: " + task.getSkill() + ", Load: " + task.getLoad());
            }
            System.out.println();
        }
        System.out.print("Employee with Highest Load: ");
        Employee highestLoadEmployee = employeeManager.getEmployeeWithHighestLoad();
        if (highestLoadEmployee != null) {
            System.out.println("Employee ID: " + highestLoadEmployee.getId() + ", Load: " + highestLoadEmployee.getLoad().get());
        } else {
            System.out.println("No employees available.");
        }

        System.out.println("Employee with Lowest Load: ");
        Employee lowestLoadEmployee = employeeManager.getEmployeeWithLowestLoad();
        if (lowestLoadEmployee != null) {
            System.out.println("Employee ID: " + lowestLoadEmployee.getId() + ", Load: " + lowestLoadEmployee.getLoad().get());
        } else {
            System.out.println("No employees available.");
        }
    }
}
