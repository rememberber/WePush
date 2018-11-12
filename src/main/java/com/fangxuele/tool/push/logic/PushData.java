package com.fangxuele.tool.push.logic;

import java.util.List;

/**
 * 推送数据
 * <br/>报表相关等使用
 * Created by rememberber(https://github.com/rememberber) on 2017/6/19.
 */
public class PushData {

    /**
     * 导入的用户
     */
    public static List<String[]> allUser;

    /**
     * 总记录数
     */
    public static long totalRecords;

    /**
     * 发送成功数
     */
    public static long successRecords;

    /**
     * 发送失败数
     */
    public static long failRecords;

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
    public static int threadCount;

    /**
     * 已经停止了的线程总数
     */
    public static int stopedThreadCount;

    /**
     * 成功数+1
     */
    synchronized public static void increaseSuccess() {
        successRecords++;
    }

    /**
     * 失败数+1
     */
    synchronized public static void increaseFail() {
        failRecords++;
    }

    /**
     * 停止线程数+1
     */
    synchronized public static void increaseStopedThread() {
        stopedThreadCount++;
    }

}
