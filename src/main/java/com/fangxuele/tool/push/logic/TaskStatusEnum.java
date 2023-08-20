package com.fangxuele.tool.push.logic;

/**
 * <pre>
 * 任务状态常量
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2023/8/14.
 */
public enum TaskStatusEnum {
    /**
     * 任务状态
     */
    INIT(10, "初始化"),
    RUNNING(20, "运行中"),
    FINISH(30, "完成"),
    STOP(40, "停止");

    private int code;

    private String name;

    public static final int INIT_CODE = 10;
    public static final int RUNNING_CODE = 20;
    public static final int FINISH_CODE = 30;
    public static final int STOP_CODE = 40;

    TaskStatusEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getName(TaskStatusEnum taskStatusEnum) {
        return taskStatusEnum.name;
    }

    public static String getDescByCode(Integer taskPeriod) {
        for (TaskStatusEnum taskStatusEnum : TaskStatusEnum.values()) {
            if (taskStatusEnum.code == taskPeriod) {
                return taskStatusEnum.name;
            }
        }
        return null;
    }

    public int getCode() {
        return code;
    }
}
