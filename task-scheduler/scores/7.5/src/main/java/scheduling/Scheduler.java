package scheduling;

import employee.Employee;
import employee.EmployeeManager;
import task.Task;
import task.TaskManager;


public class Scheduler {
    private final EmployeeManager employeeManager;
    private final TaskManager taskManager;

    public Scheduler(EmployeeManager employeeManager, TaskManager taskManager) {
        this.employeeManager = employeeManager;
        this.taskManager = taskManager;
    }

    public void scheduleTask(Task task) throws ScheduleException {
        if (task == null || task.getSkill() == null || task.getLoad() <= 0) {
            throw new ScheduleException("Task cannot be null or have invalid skill or load");
        }

        synchronized (task) {
            if (task.getStatus() != Task.TaskStatus.PENDING) {
                throw new ScheduleException("Task is already assigned");
            }
            // assign employee
            Employee employee = employeeManager.assignEmployee(task);
            if (employee == null) {
                throw new ScheduleException("No available employee for task with skill " + task.getSkill() + " and load " + task.getLoad());
            }
            try {
                taskManager.assignTask(task, employee);
            } catch (ScheduleException | RuntimeException e) {
                // Rollback employee load if task assignment fails
                employeeManager.rollbackAssignment(employee, task);
                throw e;
            }
        }
    }

    public void report() {
        System.out.println("Employee Load Report:");
        for (Employee employee : employeeManager.getEmployees()) {
            System.out.println("Employee ID: " + employee.getId() + ", Load: " + employee.getLoad());
            System.out.println("Assigned Tasks:");
            for (Task task : taskManager.getTasksByEmployee(employee)) {
                System.out.println("Task ID: " + task.getId() + ", Skill: " + task.getSkill() + ", Load: " + task.getLoad());
            }
            System.out.println();
        }
        System.out.print("Employee with Highest Load: ");
        Employee highestLoadEmployee = employeeManager.getEmployeeWithHighestLoad();
        if (highestLoadEmployee != null) {
            System.out.println("Employee ID: " + highestLoadEmployee.getId() + ", Load: " + highestLoadEmployee.getLoad());
        } else {
            System.out.println("No employees available.");
        }

        System.out.println("Employee with Lowest Load: ");
        Employee lowestLoadEmployee = employeeManager.getEmployeeWithLowestLoad();
        if (lowestLoadEmployee != null) {
            System.out.println("Employee ID: " + lowestLoadEmployee.getId() + ", Load: " + lowestLoadEmployee.getLoad());
        } else {
            System.out.println("No employees available.");
        }
    }
}
