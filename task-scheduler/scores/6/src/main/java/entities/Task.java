package entities;

import lombok.Getter;

import java.util.UUID;

@Getter
public class Task {
    private final String id;
    private final Skill skill;
    private final int load;
    private Employee assignedEmployee;
    private TaskStatus status;

    public Task(Skill skill, int load) {
        this.id = UUID.randomUUID().toString();
        this.skill = skill;
        this.load = load;
        this.assignedEmployee = null;
        this.status = TaskStatus.PENDING;
    }

    public enum TaskStatus {
        PENDING,
        ASSIGNED
    }

    public void setEmployee(Employee assignedEmployee) {
        this.assignedEmployee = assignedEmployee;
    }

    public void setTaskStatus(TaskStatus status) {
        this.status = status;
    }
}
