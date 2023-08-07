package com.fangxuele.tool.push.logic;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.hutool.cron.pattern.CronPatternUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.dao.TPeopleDataMapper;
import com.fangxuele.tool.push.dao.TTaskHisMapper;
import com.fangxuele.tool.push.dao.TTaskMapper;
import com.fangxuele.tool.push.domain.*;
import com.fangxuele.tool.push.logic.msgsender.IMsgSender;
import com.fangxuele.tool.push.logic.msgsender.MailMsgSender;
import com.fangxuele.tool.push.logic.msgsender.MsgSenderFactory;
import com.fangxuele.tool.push.logic.msgthread.MsgSendThread;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.form.*;
import com.fangxuele.tool.push.util.ConsoleUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.fangxuele.tool.push.util.SystemUtil;
import com.opencsv.CSVWriter;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

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

    private TMsg tMsg;

    // TODO 注意相关资源删除或者关闭的时候，进行清理回收
    public static Map<Integer, TaskRunThread> taskRunThreadMap = new ConcurrentHashMap<>();

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
        tMsg = msgMapper.selectByPrimaryKey(tTask.getMessageId());
        ThreadPoolExecutor threadPoolExecutor = shardingAndMsgThread(tMsg);

        taskRunThreadMap.put(taskHis.getId(), this);
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
        ConsoleUtil.consoleWithLog("线程池大小：" + tTask.getThreadCnt());

        // 线程数
        threadCount = tTask.getThreadCnt();

        taskHis.setStatus(10);

        String nowDateForSqlite = SqliteUtil.nowDateForSqlite();
        taskHis.setStartTime(nowDateForSqlite);
        taskHis.setCreateTime(nowDateForSqlite);
        taskHis.setModifiedTime(nowDateForSqlite);

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

        int maxThreadPoolSize = tTask.getThreadCnt();
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
                taskHis.setEndTime(SqliteUtil.nowDateForSqlite());

                int successCount = sendSuccessList.size();
                int failCount = sendFailList.size();
                taskHis.setSuccessCnt(successCount);
                taskHis.setFailCnt(failCount);

                taskHisMapper.updateByPrimaryKey(taskHis);

                TaskForm taskForm = TaskForm.getInstance();
                int selectedRow = taskForm.getTaskListTable().getSelectedRow();
                Integer selectedTaskId = (Integer) taskForm.getTaskListTable().getValueAt(selectedRow, 0);
                if (selectedTaskId.equals(taskId)) {
                    // 遍历TaskListTable找到taskHisId对应的行号
                    int taskHisId = taskHis.getId();
                    int taskHisListTableRows = taskForm.getTaskHisListTable().getRowCount();
                    int taskHisListTableRow = -1;
                    for (int i = 0; i < taskHisListTableRows; i++) {
                        int taskHisIdInTable = (int) taskForm.getTaskHisListTable().getValueAt(i, 0);
                        if (taskHisId == taskHisIdInTable) {
                            taskHisListTableRow = i;
                            break;
                        }
                    }
                    if (taskHisListTableRow != -1) {
                        taskForm.getTaskHisListTable().setValueAt(taskHis.getStatus(), taskHisListTableRow, 5);
                        taskForm.getTaskHisListTable().setValueAt(taskHis.getSuccessCnt(), taskHisListTableRow, 4);
                        taskForm.getTaskHisListTable().setValueAt(taskHis.getEndTime(), taskHisListTableRow, 2);
                    }
                }

                if (!fixRateScheduling) {

                    if (App.trayIcon != null) {
                        MessageEditForm messageEditForm = MessageEditForm.getInstance();
                        String msgName = messageEditForm.getMsgNameField().getText();
                        App.trayIcon.displayMessage("WePush", msgName + " 发送完毕！", TrayIcon.MessageType.INFO);
                    }

                    pushForm.getScheduleDetailLabel().setVisible(false);
                } else {
                    if (App.config.isRadioCron()) {
                        Date nextDate = CronPatternUtil.nextDateAfter(new CronPattern(App.config.getTextCron()), new Date(), true);
                        pushForm.getScheduleDetailLabel().setText("计划任务执行中，下一次执行时间：" + DateFormatUtils.format(nextDate, "yyyy-MM-dd HH:mm:ss"));
                    }
                }

                running = false;

                // 保存停止前的数据
                try {
                    // 空跑控制
                    if (dryRun == 0) {
                        ConsoleUtil.consoleWithLog("正在保存结果数据……");
                        savePushData();
                        ConsoleUtil.consoleWithLog("结果数据保存完毕！");
                    }
                } catch (IOException e) {
                    logger.error(e);
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

            TaskForm taskForm = TaskForm.getInstance();
            int selectedRow = taskForm.getTaskListTable().getSelectedRow();
            Integer selectedTaskId = (Integer) taskForm.getTaskListTable().getValueAt(selectedRow, 0);
            if (selectedTaskId.equals(taskId)) {
                // 遍历TaskListTable找到taskHisId对应的行号
                int taskHisId = taskHis.getId();
                int taskHisListTableRows = taskForm.getTaskHisListTable().getRowCount();
                int taskHisListTableRow = -1;
                for (int i = 0; i < taskHisListTableRows; i++) {
                    int taskHisIdInTable = (int) taskForm.getTaskHisListTable().getValueAt(i, 0);
                    if (taskHisId == taskHisIdInTable) {
                        taskHisListTableRow = i;
                        break;
                    }
                }
                if (taskHisListTableRow != -1) {
                    taskForm.getTaskHisListTable().setValueAt(taskHis.getSuccessCnt(), taskHisListTableRow, 4);
                }
            }
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

    private void savePushData() throws IOException {
        File pushHisDir = new File(SystemUtil.CONFIG_HOME + "data" + File.separator + "push_his");
        if (!pushHisDir.exists()) {
            boolean mkdirs = pushHisDir.mkdirs();
        }

        String taskName = tTask.getTitle();
        String nowTime = DateUtil.now().replace(":", "_").replace(" ", "_");
        CSVWriter writer;
        int msgType = App.config.getMsgType();

        List<File> fileList = new ArrayList<>();
        // 保存已发送
        if (sendSuccessList.size() > 0) {
            File sendSuccessFile = new File(SystemUtil.CONFIG_HOME + "data" +
                    File.separator + "push_his" + File.separator + MessageTypeEnum.getName(msgType) + "-" + taskName +
                    "-发送成功-" + nowTime + ".csv");
            FileUtil.touch(sendSuccessFile);
            writer = new CSVWriter(new FileWriter(sendSuccessFile));

            for (String[] str : sendSuccessList) {
                writer.writeNext(str);
            }
            writer.close();

            taskHis.setSuccessFilePath(sendSuccessFile.getAbsolutePath());
            taskHisMapper.updateByPrimaryKey(taskHis);
            fileList.add(sendSuccessFile);
            // 保存累计推送总数
            App.config.setPushTotal(App.config.getPushTotal() + sendSuccessList.size());
            App.config.save();
        }

        // 保存未发送
        for (String[] str : sendSuccessList) {
            if (msgType == MessageTypeEnum.HTTP_CODE && PushControl.saveResponseBody) {
                str = ArrayUtils.remove(str, str.length - 1);
                String[] finalStr = str;
                toSendList = toSendList.stream().filter(strings -> !JSONUtil.toJsonStr(strings).equals(JSONUtil.toJsonStr(finalStr))).collect(Collectors.toList());
            } else {
                toSendList.remove(str);
            }
        }
        for (String[] str : sendFailList) {
            if (msgType == MessageTypeEnum.HTTP_CODE && PushControl.saveResponseBody) {
                str = ArrayUtils.remove(str, str.length - 1);
                String[] finalStr = str;
                toSendList = toSendList.stream().filter(strings -> !JSONUtil.toJsonStr(strings).equals(JSONUtil.toJsonStr(finalStr))).collect(Collectors.toList());
            } else {
                toSendList.remove(str);
            }
        }

        if (toSendList.size() > 0) {
            File unSendFile = new File(SystemUtil.CONFIG_HOME + "data" + File.separator +
                    "push_his" + File.separator + MessageTypeEnum.getName(msgType) + "-" + taskName + "-未发送-" + nowTime +
                    ".csv");
            FileUtil.touch(unSendFile);
            writer = new CSVWriter(new FileWriter(unSendFile));
            for (String[] str : toSendList) {
                writer.writeNext(str);
            }
            writer.close();

            taskHis.setNoSendFilePath(unSendFile.getAbsolutePath());
            taskHisMapper.updateByPrimaryKey(taskHis);
            fileList.add(unSendFile);
        }

        // 保存发送失败
        if (sendFailList.size() > 0) {
            File failSendFile = new File(SystemUtil.CONFIG_HOME + "data" + File.separator +
                    "push_his" + File.separator + MessageTypeEnum.getName(msgType) + "-" + taskName + "-发送失败-" + nowTime + ".csv");
            FileUtil.touch(failSendFile);
            writer = new CSVWriter(new FileWriter(failSendFile));
            for (String[] str : sendFailList) {
                writer.writeNext(str);
            }
            writer.close();

            taskHis.setFailFilePath(failSendFile.getAbsolutePath());
            taskHisMapper.updateByPrimaryKey(taskHis);
            fileList.add(failSendFile);
        }

        PushHisForm.init();

        // 发送推送结果邮件
        if ((PushData.scheduling || fixRateScheduling)
                && ScheduleForm.getInstance().getSendPushResultCheckBox().isSelected()) {
            ConsoleUtil.consoleWithLog("发送推送结果邮件开始");
            String mailResultTo = ScheduleForm.getInstance().getMailResultToTextField().getText().replace("；", ";").replace(" ", "");
            String[] mailTos = mailResultTo.split(";");
            ArrayList<String> mailToList = new ArrayList<>(Arrays.asList(mailTos));

            MailMsgSender mailMsgSender = new MailMsgSender();
            String title = "WePush推送结果：【" + taskName
                    + "】" + sendSuccessList.size() + "成功；" + sendFailList.size() + "失败；"
                    + toSendList.size() + "未发送";
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("<h2>WePush推送结果</h2>");
            contentBuilder.append("<p>消息类型：").append(MessageTypeEnum.getName(App.config.getMsgType())).append("</p>");
            contentBuilder.append("<p>消息名称：").append(taskName).append("</p>");
            contentBuilder.append("<br/>");

            contentBuilder.append("<p style='color:green'><strong>成功数：").append(sendSuccessList.size()).append("</strong></p>");
            contentBuilder.append("<p style='color:red'><strong>失败数：").append(sendFailList.size()).append("</strong></p>");
            contentBuilder.append("<p>未推送数：").append(toSendList.size()).append("</p>");
            contentBuilder.append("<br/>");

            contentBuilder.append("<p>开始时间：").append(DateFormatUtils.format(new Date(startTime), "yyyy-MM-dd HH:mm:ss")).append("</p>");
            contentBuilder.append("<p>完毕时间：").append(DateFormatUtils.format(new Date(endTime), "yyyy-MM-dd HH:mm:ss")).append("</p>");
            contentBuilder.append("<p>总耗时：").append(DateUtil.formatBetween(endTime - startTime, BetweenFormatter.Level.SECOND)).append("</p>");
            contentBuilder.append("<br/>");

            contentBuilder.append("<p>详情请查看附件</p>");

            contentBuilder.append("<br/>");
            contentBuilder.append("<hr/>");
            contentBuilder.append("<p>来自WePush，一款专注于批量推送的小而美的工具</p>");
            contentBuilder.append("<img alt=\"WePush\" src=\"" + UiConsts.INTRODUCE_QRCODE_URL + "\">");

            File[] files = new File[fileList.size()];
            fileList.toArray(files);
            mailMsgSender.sendPushResultMail(mailToList, title, contentBuilder.toString(), files);
            ConsoleUtil.consoleWithLog("发送推送结果邮件结束");
        }
    }

}