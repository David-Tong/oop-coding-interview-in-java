package scheduling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleExceptionTest {
    @Test
    void preservesFailureMessage() {
        ScheduleException exception = new ScheduleException("No matching skill");
        assertEquals("No matching skill", exception.getMessage());
        assertNull(exception.getCause());
    }
}
