import entities.Employee;
import entities.Skill;
import entities.Task;
import entities.TaskFactory;
import managers.EmployeeManager;
import managers.Scheduler;
import managers.TaskManager;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class Team {
    public static void main(String[] args) throws InterruptedException {
        EmployeeManager employeeManager = new EmployeeManager();
        TaskManager taskManager = new TaskManager();

        Employee employee1 = new Employee("1", Set.of(Skill.A, Skill.B));
        Employee employee2 = new Employee("2", Set.of(Skill.B, Skill.C));
        Employee employee3 = new Employee("3", Set.of(Skill.A, Skill.C));
        Employee employee4 = new Employee("4", Set.of(Skill.A, Skill.B, Skill.C));
        Employee employee5 = new Employee("5", Set.of(Skill.B));

        employeeManager.addEmployee(employee1);
        employeeManager.addEmployee(employee2);
        employeeManager.addEmployee(employee3);
        employeeManager.addEmployee(employee4);
        employeeManager.addEmployee(employee5);

        Scheduler scheduler = new Scheduler(employeeManager, taskManager);
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(500);
        IntStream.range(0, 500).forEach(
                idx -> pool.submit(
                        () -> {
                            IntStream.range(0, 10).forEach(
                                    idx2 -> {
                                        Task task = TaskFactory.randomTask();
                                        try {
                                            scheduler.scheduleTask(task);
                                            System.out.println("Scheduled task: " + task.getId() + " with skill: " + task.getSkill() + " and load: " + task.getLoad());
                                        } catch (Exception e) {
                                            System.err.println("Failed to schedule task: " + task.getId() + " with skill: " + task.getSkill() + " and load: " + task.getLoad());
                                        }
                                    }
                            );
                            latch.countDown();
                        }));
        latch.await();
        scheduler.report();
        pool.shutdown();
    }
}
