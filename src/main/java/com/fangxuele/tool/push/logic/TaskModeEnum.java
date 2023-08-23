package com.fangxuele.tool.push.logic;

/**
 * <pre>
 * 任务模式常量
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2023/8/2
 */
public enum TaskModeEnum {
    /**
     * 任务模式
     */
    FIX_THREAD_TASK(1, "固定线程"),
    INFINITY_TASK(2, "变速模式");

    private int code;

    private String name;

    public static final int FIX_THREAD_TASK_CODE = 1;
    public static final int INFINITY_TASK_CODE = 2;

    TaskModeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getName(TaskModeEnum messageTypeEnum) {
        return messageTypeEnum.name;
    }

    public static String getDescByCode(Integer code) {
        for (TaskModeEnum taskModeEnum : TaskModeEnum.values()) {
            if (taskModeEnum.code == code) {
                return taskModeEnum.name;
            }
        }
        return null;
    }
}
