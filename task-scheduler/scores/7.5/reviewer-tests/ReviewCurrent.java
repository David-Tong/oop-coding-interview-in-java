import employee.*;
import scheduling.*;
import task.*;
import java.util.*;
import java.util.concurrent.*;

public class ReviewCurrent {
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 2000; round++) {
                EmployeeManager em = new EmployeeManager();
                Employee a = new Employee("a", Set.of(Skill.A));
                Employee b = new Employee("b", Set.of(Skill.A));
                em.addEmployee(a);
                em.addEmployee(b);
                TaskManager tm = new TaskManager();
                Task task = new Task(Skill.A, 3);
                CyclicBarrier start = new CyclicBarrier(2);
                Callable<Integer> submit = () -> {
                    start.await(5, TimeUnit.SECONDS);
                    try {
                        new Scheduler(em, tm).scheduleTask(task);
                        return 1;
                    } catch (ScheduleException rejected) { return 0; }
                };
                Future<Integer> first = pool.submit(submit);
                Future<Integer> second = pool.submit(submit);
                check(first.get(5, TimeUnit.SECONDS) + second.get(5, TimeUnit.SECONDS) == 1);
                check(tm.getTasksByEmployee(a).size() + tm.getTasksByEmployee(b).size() == 1);
                for (Employee e : List.of(a, b)) {
                    check(e.getLoad() == tm.getTasksByEmployee(e).stream().mapToInt(Task::getLoad).sum());
                    for (Task t : tm.getTasksByEmployee(e)) check(t.getAssignedEmployee() == e);
                }
                check(task.getStatus() == Task.TaskStatus.ASSIGNED);
            }
            System.out.println("PASS: 2000 duplicate-submission rounds, two employees and separate schedulers");

            TaskManager tm = new TaskManager();
            Employee e = new Employee("reader", Set.of(Skill.A));
            tm.assignTask(new Task(Skill.A, 1), e);
            List<Task> old = tm.getTasksByEmployee(e);
            CountDownLatch start = new CountDownLatch(1);
            Future<?> writer = pool.submit(() -> {
                start.await();
                for (int i = 0; i < 1000; i++) tm.assignTask(new Task(Skill.A, 1), e);
                return null;
            });
            Future<?> reader = pool.submit(() -> {
                start.await();
                for (int i = 0; i < 1000; i++) {
                    List<Task> snapshot = tm.getTasksByEmployee(e);
                    int size = snapshot.size();
                    for (Task t : snapshot) check(t.getAssignedEmployee() == e);
                    check(snapshot.size() == size);
                }
                return null;
            });
            start.countDown();
            writer.get(10, TimeUnit.SECONDS);
            reader.get(10, TimeUnit.SECONDS);
            check(old.size() == 1);
            check(tm.getTasksByEmployee(e).size() == 1001);
            try { old.clear(); throw new AssertionError("mutable snapshot"); }
            catch (UnsupportedOperationException expected) { }
            System.out.println("PASS: concurrent task-list queries and stable immutable membership snapshots");
        } finally {
            pool.shutdownNow();
            check(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("invariant failed");
    }
}
