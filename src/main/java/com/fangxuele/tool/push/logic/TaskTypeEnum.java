package com.fangxuele.tool.push.logic;

/**
 * <pre>
 * 任务类型常量
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2021/6/4.
 */
public enum TaskTypeEnum {
    /**
     * 任务类型
     */
    MANUAL_TASK(1, "手动任务"),
    SCHEDULE_TASK(2, "定时任务"),
    TRIGGER_TASK(3, "触发任务");

    private int code;

    private String name;

    public static final int MANUAL_TASK_CODE = 1;
    public static final int SCHEDULE_TASK_CODE = 2;
    public static final int TRIGGER_TASK_CODE = 3;

    TaskTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getName(TaskTypeEnum messageTypeEnum) {
        return messageTypeEnum.name;
    }

    public static String getDescByCode(Integer taskPeriod) {
        for (TaskTypeEnum taskTypeEnum : TaskTypeEnum.values()) {
            if (taskTypeEnum.code == taskPeriod) {
                return taskTypeEnum.name;
            }
        }
        return null;
    }
}
