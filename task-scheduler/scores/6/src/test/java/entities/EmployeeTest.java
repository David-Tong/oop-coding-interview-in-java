package entities;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class EmployeeTest {
    @Test
    void startsWithZeroLoadAndExposesImmutableSkills() {
        Employee employee = new Employee("e1", Set.of(Skill.A, Skill.C));
        assertEquals("e1", employee.getId());
        assertEquals(0, employee.getLoad().get());
        assertEquals(Set.of(Skill.A, Skill.C), employee.getSkillSet());
        assertThrows(UnsupportedOperationException.class, () -> employee.getSkillSet().add(Skill.B));
    }

    @Test
    void comparesCurrentWorkloadsIncludingTies() {
        Employee first = new Employee("first", Set.of(Skill.A));
        Employee second = new Employee("second", Set.of(Skill.B));
        assertEquals(0, first.compareTo(second));
        second.getLoad().set(5);
        assertTrue(first.compareTo(second) < 0);
        assertTrue(second.compareTo(first) > 0);
        first.getLoad().set(8);
        assertTrue(first.compareTo(second) > 0);
    }
}
