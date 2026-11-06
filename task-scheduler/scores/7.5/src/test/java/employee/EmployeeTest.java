package employee;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class EmployeeTest {
    @Test
    void startsWithZeroLoadAndExposesImmutableSkills() {
        Employee employee = new Employee("e1", Set.of(Skill.A, Skill.C));
        assertEquals("e1", employee.getId());
        assertEquals(0, employee.getLoad());
        assertEquals(Set.of(Skill.A, Skill.C), employee.getSkillSet());
        assertThrows(UnsupportedOperationException.class, () -> employee.getSkillSet().add(Skill.B));
    }

    @Test
    void addsAndRemovesWorkload() {
        Employee employee = new Employee("employee", Set.of(Skill.A));
        employee.addLoad(5);
        employee.addLoad(3);
        assertEquals(8, employee.getLoad());
        employee.removeLoad(3);
        assertEquals(5, employee.getLoad());
        employee.removeLoad(5);
        assertEquals(0, employee.getLoad());
    }
}
