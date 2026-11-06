import employee.*;
import scheduling.*;
import task.*;
import java.util.*;
import java.util.concurrent.*;

/** Standalone observations against the current API; does not depend on stale tests. */
public class ReviewCurrent {
    public static void main(String[] args) throws Exception {
        EmployeeManager manager = new EmployeeManager();
        Employee a = new Employee("a", Set.of(Skill.A, Skill.B));
        Employee b = new Employee("b", Set.of(Skill.A, Skill.B));
        manager.addEmployee(a);
        manager.addEmployee(b);
        TaskManager tasks = new TaskManager();
        Scheduler scheduler = new Scheduler(manager, tasks);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                Skill skill = i % 2 == 0 ? Skill.A : Skill.B;
                futures.add(pool.submit(() -> { scheduler.scheduleTask(new Task(skill, 3)); return null; }));
            }
            for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        if (a.getLoad() != 300 || b.getLoad() != 300) throw new AssertionError("balance");
        if (tasks.getTasksByEmployee(a).size() + tasks.getTasksByEmployee(b).size() != 200)
            throw new AssertionError("records");
        System.out.println("PASS concurrent cross-skill balance: 300,300; records=200");

        EmployeeManager single = new EmployeeManager();
        Employee e = new Employee("e", Set.of(Skill.A));
        single.addEmployee(e);
        TaskManager records = new TaskManager();
        Scheduler s = new Scheduler(single, records);
        Task task = new Task(Skill.A, 3);
        s.scheduleTask(task);
        try { s.scheduleTask(task); throw new AssertionError("duplicate accepted"); }
        catch (ScheduleException expected) { }
        if (e.getLoad() != 3 || records.getTasksByEmployee(e).size() != 1)
            throw new AssertionError("duplicate changed original accounting");
        System.out.println("Duplicate rejected: load=" + e.getLoad() + ", recordedLoad=" +
                records.getTasksByEmployee(e).stream().mapToInt(Task::getLoad).sum());

        EmployeeManager failing = new EmployeeManager();
        Employee f = new Employee("f", Set.of(Skill.A));
        failing.addEmployee(f);
        TaskManager storage = new TaskManager() {
            @Override public void assignTask(Task t, Employee employee) { throw new IllegalStateException("before record"); }
        };
        Task pending = new Task(Skill.A, 4);
        try { new Scheduler(failing, storage).scheduleTask(pending); throw new AssertionError("failure swallowed"); }
        catch (ScheduleException expected) { }
        if (f.getLoad() != 0 || !storage.getTasksByEmployee(f).isEmpty()
                || pending.getStatus() != Task.TaskStatus.PENDING)
            throw new AssertionError("pre-record failure was not restored");
        System.out.println("Recording failure: load=" + f.getLoad() + ", records=" +
                storage.getTasksByEmployee(f).size() + ", status=" + pending.getStatus());
    }
}
