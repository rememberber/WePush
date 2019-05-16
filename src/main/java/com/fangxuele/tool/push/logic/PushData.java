package com.fangxuele.tool.push.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

/**
 * <pre>
 * 推送数据 报表相关等使用
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/19.
 */
public class PushData {

    /**
     * 导入的用户
     */
    public static List<String[]> allUser = Collections.synchronizedList(new ArrayList<>());

    /**
     * 总记录数
     */
    static long totalRecords;

    /**
     * 发送成功数
     */
    public static LongAdder successRecords = new LongAdder();

    /**
     * 发送失败数
     */
    public static LongAdder failRecords = new LongAdder();

    /**
     * 准备发送的列表
     */
    static List<String[]> toSendList;

    /**
     * 发送成功的列表
     */
    public static List<String[]> sendSuccessList;

    /**
     * 发送失败的列表
     */
    public static List<String[]> sendFailList;

    /**
     * 停止标志
     */
    public volatile static boolean running = false;

    /**
     * 计划任务执行中标志
     */
    public static boolean scheduling = false;

    /**
     * 固定频率计划任务执行中
     */
    public static boolean fixRateScheduling = false;

    /**
     * 线程总数
     */
    static int threadCount;

    /**
     * 已经停止了的线程总数
     */
    static LongAdder stopedThreadCount = new LongAdder();
    ;

    /**
     * 成功数+1
     */
    static void increaseSuccess() {
        successRecords.add(1);
    }

    /**
     * 失败数+1
     */
    static void increaseFail() {
        failRecords.add(1);
    }

    /**
     * 停止线程数+1
     */
    public static void increaseStopedThread() {
        stopedThreadCount.add(1);
    }

}
