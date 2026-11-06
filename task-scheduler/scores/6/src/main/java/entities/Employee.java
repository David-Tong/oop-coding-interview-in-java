package entities;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
@Getter
public class Employee implements Comparable<Employee>{
    private final String id;
    private final AtomicInteger load = new AtomicInteger();
    @Getter(AccessLevel.NONE)
    private Set<Skill> skillSet;

    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.load.get(), other.load.get());
    }

    public Set<Skill> getSkillSet() {
        return Set.copyOf(skillSet);
    }
}
