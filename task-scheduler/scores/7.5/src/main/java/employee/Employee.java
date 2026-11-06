package employee;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Employee {
    private final String id;
    private int load;
    private Set<Skill> skillSet;

    public Employee(String id, Set<Skill> skillSet) {
        this.id = id;
        this.load = 0;
        this.skillSet = Set.copyOf(skillSet);
    }

    public String getId() {
        return id;
    }

    public int getLoad() {
        return load;
    }

    void addLoad(int workload) {
        load += workload;
    }

    void removeLoad(int workload) {
        load -= workload;
    }

    Set<Skill> getSkillSet() {
        return Set.copyOf(skillSet);
    }
}
