package com.fangxuele.tool.push.logic;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.hutool.cron.pattern.CronPatternUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.dao.TPeopleDataMapper;
import com.fangxuele.tool.push.dao.TTaskHisMapper;
import com.fangxuele.tool.push.dao.TTaskMapper;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TPeopleData;
import com.fangxuele.tool.push.domain.TTask;
import com.fangxuele.tool.push.domain.TTaskHis;
import com.fangxuele.tool.push.logic.msgsender.IMsgSender;
import com.fangxuele.tool.push.logic.msgsender.MsgSenderFactory;
import com.fangxuele.tool.push.logic.msgthread.MsgSendThread;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.PushForm;
import com.fangxuele.tool.push.ui.form.TaskForm;
import com.fangxuele.tool.push.util.ConsoleUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.LongAdder;

/**
 * <pre>
 * 推送执行控制线程
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2023/8/03
 */
@Getter
@Setter
public class TaskRunThread extends Thread {

    private static final Log logger = LogFactory.get();

    private Integer taskId;

    private Integer dryRun;

    /**
     * 发送成功数
     */
    public LongAdder successRecords = new LongAdder();

    /**
     * 发送失败数
     */
    public LongAdder failRecords = new LongAdder();

    /**
     * 停止标志
     */
    public volatile boolean running = false;

    private Long startTime;

    /**
     * 结束时间
     */
    public static long endTime = 0;

    private List<String[]> toSendList;

    /**
     * 总记录数
     */
    static long totalRecords;

    /**
     * 线程总数
     */
    public int threadCount;

    /**
     * 固定频率计划任务执行中
     */
    public boolean fixRateScheduling = false;

    /**
     * 发送成功的列表
     */
    public List<String[]> sendSuccessList;

    /**
     * 发送失败的列表
     */
    public List<String[]> sendFailList;

    private TTask tTask;

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Integer getDryRun() {
        return dryRun;
    }

    public void setDryRun(Integer dryRun) {
        this.dryRun = dryRun;
    }

    private static TTaskMapper taskMapper = MybatisUtil.getSqlSession().getMapper(TTaskMapper.class);
    private static TTaskHisMapper taskHisMapper = MybatisUtil.getSqlSession().getMapper(TTaskHisMapper.class);

    private static TPeopleDataMapper peopleDataMapper = MybatisUtil.getSqlSession().getMapper(TPeopleDataMapper.class);

    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private TTaskHis taskHis;

    public TaskRunThread(Integer taskId, Integer dryRun) {
        this.taskId = taskId;
        this.dryRun = dryRun;
    }

    @Override
    public void run() {
        // 准备推送
        this.tTask = taskMapper.selectByPrimaryKey(taskId);

        preparePushRun(tTask);
        ConsoleUtil.consoleWithLog("推送开始……");
        // 消息数据分片以及线程纷发
        TMsg tMsg = msgMapper.selectByPrimaryKey(tTask.getMessageId());
        ThreadPoolExecutor threadPoolExecutor = shardingAndMsgThread(tMsg);
        // 时间监控
        timeMonitor(threadPoolExecutor);
    }

    /**
     * 准备推送
     */
    private void preparePushRun(TTask tTask) {

        // 初始化任务历史表
        taskHis = new TTaskHis();

        taskHis.setTaskId(tTask.getId());

        // 设置是否空跑
        taskHis.setDryRun(dryRun);

        // TODO 执行前重新导入目标用户

        // 重置推送数据
        resetLocalData();

        startTime = System.currentTimeMillis();

        // 拷贝准备的目标用户
        List<TPeopleData> tPeopleData = peopleDataMapper.selectByPeopleId(tTask.getPeopleId());

        tPeopleData.forEach(peopleData -> {
            String varData = peopleData.getVarData();
            String[] strings = JSON.parseObject(varData, new TypeReference<String[]>() {
            });
            toSendList.add(strings);
        });
        // 总记录数
        totalRecords = toSendList.size();

        taskHis.setTotalCnt((int) totalRecords);
        ConsoleUtil.consoleWithLog("消息总数：" + totalRecords);
        ConsoleUtil.consoleWithLog("可用处理器核心：" + Runtime.getRuntime().availableProcessors());

        // 线程数
        ConsoleUtil.consoleWithLog("线程数：" + tTask.getThreadCnt());

        ConsoleUtil.consoleWithLog("线程池大小：" + App.config.getMaxThreads());

        // 线程数
        threadCount = tTask.getThreadCnt();

        taskHis.setStartTime(SqliteUtil.nowDateForSqlite());

        taskHis.setStatus(10);

        taskHisMapper.insert(taskHis);

        TaskForm taskForm = TaskForm.getInstance();
        int selectedRow = taskForm.getTaskListTable().getSelectedRow();
        Integer selectedTaskId = (Integer) taskForm.getTaskListTable().getValueAt(selectedRow, 0);
        if (selectedTaskId.equals(taskId)) {
            TaskForm.initTaskHisListTable(taskId);
        }
    }

    /**
     * 消息数据分片以及线程纷发
     */
    private ThreadPoolExecutor shardingAndMsgThread(TMsg tMsg) {

        int maxThreadPoolSize = App.config.getMaxThreads();
        ThreadPoolExecutor threadPoolExecutor = ThreadUtil.newExecutor(maxThreadPoolSize, maxThreadPoolSize);
        MsgSendThread msgSendThread;
        // 每个线程分配
        int perThread = (int) (totalRecords / threadCount) + 1;
        for (int i = 0; i < threadCount; i++) {
            int startIndex = i * perThread;
            if (startIndex > totalRecords - 1) {
                threadCount = i;
                break;
            }
            int endIndex = i * perThread + perThread;
            if (endIndex > totalRecords - 1) {
                endIndex = (int) (totalRecords);
            }

            IMsgSender msgSender = MsgSenderFactory.getMsgSender(tMsg.getId(), dryRun);
            msgSendThread = new MsgSendThread(startIndex, endIndex, msgSender, this);

            msgSendThread.setName("T-" + i);

            threadPoolExecutor.execute(msgSendThread);
        }
        threadPoolExecutor.shutdown();
        ConsoleUtil.consoleWithLog("所有线程宝宝启动完毕……");
        return threadPoolExecutor;
    }

    /**
     * 时间监控
     *
     * @param threadPoolExecutor
     */
    private void timeMonitor(ThreadPoolExecutor threadPoolExecutor) {
        PushForm pushForm = PushForm.getInstance();
        long startTimeMillis = System.currentTimeMillis();
        int totalSentCountBefore = 0;
        // 计时
        while (true) {
            if (threadPoolExecutor.isTerminated()) {
                if (!fixRateScheduling) {

                    if (App.trayIcon != null) {
                        MessageEditForm messageEditForm = MessageEditForm.getInstance();
                        String msgName = messageEditForm.getMsgNameField().getText();
                        App.trayIcon.displayMessage("WePush", msgName + " 发送完毕！", TrayIcon.MessageType.INFO);
                    }

                    String finishTip = "发送完毕！\n";
                    JOptionPane.showMessageDialog(pushForm.getPushPanel(), finishTip, "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    pushForm.getScheduleDetailLabel().setVisible(false);
                } else {
                    if (App.config.isRadioCron()) {
                        Date nextDate = CronPatternUtil.nextDateAfter(new CronPattern(App.config.getTextCron()), new Date(), true);
                        pushForm.getScheduleDetailLabel().setText("计划任务执行中，下一次执行时间：" + DateFormatUtils.format(nextDate, "yyyy-MM-dd HH:mm:ss"));
                    }
                    pushForm.getPushStopButton().setText("停止计划任务");
                }

                taskHis.setEndTime(SqliteUtil.nowDateForSqlite());

                taskHisMapper.updateByPrimaryKey(taskHis);

                // 保存停止前的数据
                try {
                    // 空跑控制
                    if (!pushForm.getDryRunCheckBox().isSelected()) {
                        ConsoleUtil.consoleWithLog("正在保存结果数据……");
                        pushForm.getPushTotalProgressBar().setIndeterminate(true);
                        PushControl.savePushData();
                        ConsoleUtil.consoleWithLog("结果数据保存完毕！");
                    }
                } catch (IOException e) {
                    logger.error(e);
                } finally {
                    pushForm.getPushTotalProgressBar().setIndeterminate(false);
                }
                break;
            }

            int successCount = sendSuccessList.size();
            int failCount = sendFailList.size();
            int totalSentCount = successCount + failCount;
            long currentTimeMillis = System.currentTimeMillis();
            long lastTimeMillis = currentTimeMillis - startTimeMillis;
            long leftTimeMillis = (long) ((double) lastTimeMillis / (totalSentCount) * (toSendList.size() - totalSentCount));

            taskHis.setSuccessCnt(successCount);
            taskHis.setFailCnt(failCount);

            // 耗时
            String formatBetweenLast = DateUtil.formatBetween(lastTimeMillis, BetweenFormatter.Level.SECOND);
            pushForm.getPushLastTimeLabel().setText("".equals(formatBetweenLast) ? "0s" : formatBetweenLast);

            // 预计剩余
            String formatBetweenLeft = DateUtil.formatBetween(leftTimeMillis, BetweenFormatter.Level.SECOND);
            pushForm.getPushLeftTimeLabel().setText("".equals(formatBetweenLeft) ? "0s" : formatBetweenLeft);

            pushForm.getJvmMemoryLabel().setText("JVM内存占用：" + FileUtil.readableFileSize(Runtime.getRuntime().totalMemory()) + "/" + FileUtil.readableFileSize(Runtime.getRuntime().maxMemory()));

            // TPS
            int tps = (totalSentCount - totalSentCountBefore) * 5;
            totalSentCountBefore = totalSentCount;
            pushForm.getTpsLabel().setText(String.valueOf(tps));

            taskHisMapper.updateByPrimaryKey(taskHis);
            ThreadUtil.safeSleep(200);
        }
    }

    /**
     * 成功数+1
     */
    public void increaseSuccess() {
        successRecords.add(1);
    }

    /**
     * 失败数+1
     */
    public void increaseFail() {
        failRecords.add(1);
    }

    private void resetLocalData() {
        running = true;
        successRecords.reset();
        failRecords.reset();
        threadCount = 0;
        toSendList = Collections.synchronizedList(new LinkedList<>());
        sendSuccessList = Collections.synchronizedList(new LinkedList<>());
        sendFailList = Collections.synchronizedList(new LinkedList<>());
        startTime = 0L;
        endTime = 0;
    }

}