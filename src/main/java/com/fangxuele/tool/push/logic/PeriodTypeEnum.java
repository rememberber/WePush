package com.fangxuele.tool.push.logic;

/**
 * <pre>
 * 周期类型常量
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2023/8/2
 */
public enum PeriodTypeEnum {
    /**
     * 周期类型
     */
    RUN_AT_THIS_TIME_TASK(1, "在此时间开始推送"),
    RUN_PER_DAY_TASK(2, "每天定时推送"),
    RUN_PER_WEEK_TASK(3, "每周定时推送"),
    CRON_TASK(4, "Cron表达式定时推送");

    private int code;

    private String name;

    public static final int RUN_AT_THIS_TIME_TASK_CODE = 1;
    public static final Integer RUN_PER_DAY_TASK_CODE = 2;
    public static final int RUN_PER_WEEK_TASK_CODE = 3;
    public static final Integer CRON_TASK_CODE = 4;

    PeriodTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getName(PeriodTypeEnum messageTypeEnum) {
        return messageTypeEnum.name;
    }

    public static String getDesc(Integer periodType) {
        for (PeriodTypeEnum periodTypeEnum : PeriodTypeEnum.values()) {
            if (periodTypeEnum.code == periodType) {
                return periodTypeEnum.name;
            }
        }
        return null;
    }
}
