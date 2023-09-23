package com.fangxuele.tool.push.logic.msgthread;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.logic.TaskRunThread;
import com.fangxuele.tool.push.util.ConsoleUtil;

import java.util.List;

/**
 * <pre>
 * 消息发送服务线程父类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/3/29.
 */
public class BaseMsgThread implements Runnable {

    public static final Log logger = LogFactory.get();

    /**
     * 起始索引
     */
    private int startIndex;

    /**
     * 截止索引
     */
    private int endIndex;

    /**
     * 当前线程要发送的list
     */
    public List<String[]> list;

    protected TaskRunThread taskRunThread;

    public static int msgType;

    public BaseMsgThread(int startIndex, int endIndex, TaskRunThread taskRunThread) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.taskRunThread = taskRunThread;
    }

    @Override
    public void run() {

    }

    /**
     * 初始化当前线程
     */
    public void initCurrentThread() {
        ConsoleUtil.pushLog(taskRunThread.getLogWriter(), "线程" + Thread.currentThread().getName() + "负责处理第:" + startIndex + "-" + endIndex + "条数据");

        list = taskRunThread.getToSendList().subList(startIndex, endIndex);

    }

    /**
     * 当前线程结束
     */
    public void currentThreadFinish() {
        ConsoleUtil.pushLog(taskRunThread.getLogWriter(), Thread.currentThread().getName() + "已处理完第" + startIndex + "-" + endIndex + "条的数据");
    }

}
