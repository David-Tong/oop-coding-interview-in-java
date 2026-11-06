package task;

import employee.Employee;
import employee.Skill;
import lombok.Getter;
import scheduling.ScheduleException;

import java.util.UUID;

public class Task {
    public enum TaskStatus {
        PENDING,
        ASSIGNED
    }

    @Getter
    private final String id;
    @Getter
    private final Skill skill;
    @Getter
    private final int load;
    @Getter
    private Employee assignedEmployee;
    @Getter
    private TaskStatus status;

    public Task(Skill skill, int load) {
        this.id = UUID.randomUUID().toString();
        this.skill = skill;
        this.load = load;
        this.assignedEmployee = null;
        this.status = TaskStatus.PENDING;
    }

    public void assignTask(Employee assignedEmployee) throws ScheduleException {
        if (this.assignedEmployee != null) {
            throw new ScheduleException("Task is already assigned to an employee. Reassignment is not allowed.");
        }
        if (this.status != TaskStatus.PENDING) {
            throw new ScheduleException("Task is not in a pending state and cannot be assigned.");
        }
        this.assignedEmployee = assignedEmployee;
        this.status = TaskStatus.ASSIGNED;
    }

}
