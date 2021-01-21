package com.fangxuele.tool.push.logic;

import cn.hutool.core.date.BetweenFormater;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.hutool.cron.pattern.CronPatternUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.msgsender.IMsgSender;
import com.fangxuele.tool.push.logic.msgsender.MsgSenderFactory;
import com.fangxuele.tool.push.logic.msgthread.BaseMsgThread;
import com.fangxuele.tool.push.logic.msgthread.MsgSendThread;
import com.fangxuele.tool.push.ui.component.TableInCellProgressBarRenderer;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.PushForm;
import com.fangxuele.tool.push.util.ConsoleUtil;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <pre>
 * 推送执行控制线程
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/28.
 */
public class PushRunThread extends Thread {

    private static final Log logger = LogFactory.get();

    @Override
    public void run() {
        PushForm pushForm = PushForm.getInstance();
        pushForm.getPushTotalProgressBar().setIndeterminate(true);
        // 准备推送
        preparePushRun();
        pushForm.getPushTotalProgressBar().setIndeterminate(false);
        ConsoleUtil.consoleWithLog("推送开始……");
        // 消息数据分片以及线程纷发
        ThreadPoolExecutor threadPoolExecutor = shardingAndMsgThread();
        // 时间监控
        timeMonitor(threadPoolExecutor);
    }

    /**
     * 准备推送
     */
    private void preparePushRun() {
        PushForm pushForm = PushForm.getInstance();
        // 按钮状态
        pushForm.getScheduleRunButton().setEnabled(false);
        pushForm.getPushStartButton().setEnabled(false);
        pushForm.getThreadCountSlider().setEnabled(false);
        pushForm.getPushStopButton().setEnabled(true);

        pushForm.getPushStopButton().setText("停止");
        // 初始化
        pushForm.getPushSuccessCount().setText("0");
        pushForm.getPushFailCount().setText("0");

        // 设置是否空跑
        PushControl.dryRun = pushForm.getDryRunCheckBox().isSelected();

        PushControl.saveResponseBody = pushForm.getSaveResponseBodyCheckBox().isSelected();

        // 执行前重新导入目标用户
        PushControl.reimportMembers();

        // 重置推送数据
        PushData.reset();
        PushData.startTime = System.currentTimeMillis();

        // 拷贝准备的目标用户
        PushData.toSendList.addAll(PushData.allUser);
        // 总记录数
        PushData.totalRecords = PushData.toSendList.size();

        pushForm.getPushTotalCountLabel().setText("消息总数：" + PushData.totalRecords);
        pushForm.getPushTotalProgressBar().setMaximum((int) PushData.totalRecords);
        ConsoleUtil.consoleWithLog("消息总数：" + PushData.totalRecords);
        // 可用处理器核心数量
        pushForm.getAvailableProcessorLabel().setText("可用处理器核心：" + Runtime.getRuntime().availableProcessors());
        ConsoleUtil.consoleWithLog("可用处理器核心：" + Runtime.getRuntime().availableProcessors());

        // 线程数
        App.config.setThreadCount(Integer.parseInt(pushForm.getThreadCountTextField().getText()));
        App.config.save();
        ConsoleUtil.consoleWithLog("线程数：" + pushForm.getThreadCountTextField().getText());

        ConsoleUtil.consoleWithLog("线程池大小：" + App.config.getMaxThreads());

        // 准备消息构造器
        PushControl.prepareMsgMaker();

        // 线程数
        PushData.threadCount = Integer.parseInt(pushForm.getThreadCountTextField().getText());

        // 初始化线程table
        String[] headerNames = {"线程", "分片区间", "成功", "失败", "总数", "当前进度"};
        DefaultTableModel tableModel = new DefaultTableModel(null, headerNames);
        pushForm.getPushThreadTable().setModel(tableModel);
        pushForm.getPushThreadTable().getColumn("当前进度").setCellRenderer(new TableInCellProgressBarRenderer());

        DefaultTableCellRenderer hr = (DefaultTableCellRenderer) pushForm.getPushThreadTable().getTableHeader()
                .getDefaultRenderer();
        // 表头列名居左
        hr.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
        pushForm.getPushThreadTable().updateUI();
    }

    /**
     * 消息数据分片以及线程纷发
     */
    private static ThreadPoolExecutor shardingAndMsgThread() {
        PushForm pushForm = PushForm.getInstance();
        Object[] data;

        int maxThreadPoolSize = App.config.getMaxThreads();
        ThreadPoolExecutor threadPoolExecutor = ThreadUtil.newExecutor(maxThreadPoolSize, maxThreadPoolSize);
        MsgSendThread msgSendThread;
        // 每个线程分配
        int perThread = (int) (PushData.totalRecords / PushData.threadCount) + 1;
        DefaultTableModel tableModel = (DefaultTableModel) pushForm.getPushThreadTable().getModel();
        BaseMsgThread.msgType = App.config.getMsgType();
        for (int i = 0; i < PushData.threadCount; i++) {
            int startIndex = i * perThread;
            if (startIndex > PushData.totalRecords - 1) {
                PushData.threadCount = i;
                break;
            }
            int endIndex = i * perThread + perThread;
            if (endIndex > PushData.totalRecords - 1) {
                endIndex = (int) (PushData.totalRecords);
            }

            IMsgSender msgSender = MsgSenderFactory.getMsgSender();
            msgSendThread = new MsgSendThread(startIndex, endIndex, msgSender);

            msgSendThread.setTableRow(i);
            msgSendThread.setName("T-" + i);

            data = new Object[6];
            data[0] = msgSendThread.getName();
            data[1] = startIndex + "-" + endIndex;
            data[5] = 0;
            tableModel.addRow(data);

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
                if (!PushData.fixRateScheduling) {
                    pushForm.getPushStopButton().setEnabled(false);
                    pushForm.getPushStopButton().updateUI();
                    pushForm.getPushStartButton().setEnabled(true);
                    pushForm.getThreadCountSlider().setEnabled(true);
                    pushForm.getPushStartButton().updateUI();
                    pushForm.getScheduleRunButton().setEnabled(true);
                    pushForm.getScheduleRunButton().updateUI();
                    pushForm.getScheduleDetailLabel().setText("");

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

                PushData.endTime = System.currentTimeMillis();

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

            int successCount = PushData.sendSuccessList.size();
            int failCount = PushData.sendFailList.size();
            int totalSentCount = successCount + failCount;
            long currentTimeMillis = System.currentTimeMillis();
            long lastTimeMillis = currentTimeMillis - startTimeMillis;
            long leftTimeMillis = (long) ((double) lastTimeMillis / (totalSentCount) * (PushData.allUser.size() - totalSentCount));

            // 耗时
            String formatBetweenLast = DateUtil.formatBetween(lastTimeMillis, BetweenFormater.Level.SECOND);
            pushForm.getPushLastTimeLabel().setText("".equals(formatBetweenLast) ? "0s" : formatBetweenLast);

            // 预计剩余
            String formatBetweenLeft = DateUtil.formatBetween(leftTimeMillis, BetweenFormater.Level.SECOND);
            pushForm.getPushLeftTimeLabel().setText("".equals(formatBetweenLeft) ? "0s" : formatBetweenLeft);

            pushForm.getJvmMemoryLabel().setText("JVM内存占用：" + FileUtil.readableFileSize(Runtime.getRuntime().totalMemory()) + "/" + FileUtil.readableFileSize(Runtime.getRuntime().maxMemory()));

            // TPS
            int tps = (totalSentCount - totalSentCountBefore) * 5;
            totalSentCountBefore = totalSentCount;
            pushForm.getTpsLabel().setText(String.valueOf(tps));
            ThreadUtil.safeSleep(200);
        }
    }

}