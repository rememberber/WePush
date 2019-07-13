package com.fangxuele.tool.push.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
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
     * (异步发送)已处理数
     */
    public static LongAdder processedRecords = new LongAdder();

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
    public static List<String[]> toSendList;

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

    /**
     * 已处理数+1
     */
    public static void increaseProcessed() {
        processedRecords.add(1);
    }

    /**
     * 成功数+1
     */
    public static void increaseSuccess() {
        successRecords.add(1);
    }

    /**
     * 失败数+1
     */
    public static void increaseFail() {
        failRecords.add(1);
    }

    /**
     * 停止线程数+1
     */
    public static void increaseStopedThread() {
        stopedThreadCount.add(1);
    }

    /**
     * 开始时间
     */
    public static long startTime = 0;

    /**
     * 结束时间
     */
    public static long endTime = 0;

    /**
     * 是否为性能模式
     */
    public static boolean boostMode = false;

    /**
     * 重置推送数据
     */
    static void reset() {
        running = true;
        processedRecords.reset();
        successRecords.reset();
        failRecords.reset();
        stopedThreadCount.reset();
        threadCount = 0;
        toSendList = Collections.synchronizedList(new LinkedList<>());
        sendSuccessList = Collections.synchronizedList(new LinkedList<>());
        sendFailList = Collections.synchronizedList(new LinkedList<>());
        startTime = 0;
        endTime = 0;
    }

}
