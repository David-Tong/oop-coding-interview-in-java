package task;

import employee.Skill;

import java.util.concurrent.ThreadLocalRandom;

public class TaskFactory {
    public static Task randomTask() {
        Skill[] skills = Skill.values();
        Skill skill = skills[ThreadLocalRandom.current().nextInt(skills.length)];
        int load = ThreadLocalRandom.current().nextInt(1, 10);
        return new Task(skill, load);
    }
}
