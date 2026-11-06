package employee;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Employee implements Comparable<Employee> {
    private final String id;
    private final AtomicInteger load = new AtomicInteger();
    private Set<Skill> skillSet;

    public Employee(String id, Set<Skill> skillSet) {
        this.id = id;
        this.skillSet = Set.copyOf(skillSet);
    }

    public String getId() {
        return id;
    }

    public int getLoad() {
        return load.get();
    }

    void setLoad(int newLoad) {
        load.getAndSet(newLoad);
    }

    void addLoad(int workload) {
        load.addAndGet(workload);
    }

    void removeLoad(int workload) {
        load.addAndGet(-workload);
    }

    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.load.get(), other.load.get());
    }

    Set<Skill> getSkillSet() {
        return Set.copyOf(skillSet);
    }


}
