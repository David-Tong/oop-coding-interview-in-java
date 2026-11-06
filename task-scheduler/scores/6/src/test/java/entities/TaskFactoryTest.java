package entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import static support.ConcurrentTestSupport.runWorkers;

@Timeout(20)
class TaskFactoryTest {
    private void verify(Task task) {
        assertNotNull(UUID.fromString(task.getId()));
        assertTrue(Set.of(Skill.values()).contains(task.getSkill()));
        assertTrue(task.getLoad() >= 1 && task.getLoad() <= 9);
        assertNull(task.getAssignedEmployee());
        assertEquals(Task.TaskStatus.PENDING, task.getStatus());
    }

    @Test
    void generatesValidPendingTasks() {
        for (int i = 0; i < 100; i++) verify(TaskFactory.randomTask());
    }

    @Test
    void concurrentGenerationProducesUniqueValidTasks() throws Exception {
        Set<String> ids = ConcurrentHashMap.newKeySet();
        runWorkers(8, worker -> {
            for (int i = 0; i < 250; i++) {
                Task task = TaskFactory.randomTask();
                verify(task);
                assertTrue(ids.add(task.getId()), "Duplicate task ID");
            }
        });
        assertEquals(2000, ids.size());
    }
}
