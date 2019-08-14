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
import com.fangxuele.tool.push.logic.msgthread.MsgAsyncSendThread;
import com.fangxuele.tool.push.ui.form.BoostForm;
import com.fangxuele.tool.push.util.ConsoleUtil;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.http.HttpResponse;

import javax.swing.*;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

/**
 * <pre>
 * 性能模式推送执行控制线程
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/7/7.
 */
public class BoostPushRunThread extends Thread {

    private static final Log logger = LogFactory.get();

    public static List<Future<HttpResponse>> futureList = Lists.newArrayList();

    @Override
    public void run() {
        BoostForm boostForm = BoostForm.getInstance();
        boostForm.getProcessedProgressBar().setIndeterminate(true);
        boostForm.getCompletedProgressBar().setIndeterminate(true);
        // 准备推送
        preparePushRun();
        boostForm.getProcessedProgressBar().setIndeterminate(false);
        boostForm.getCompletedProgressBar().setIndeterminate(false);
        ConsoleUtil.boostConsoleWithLog("推送开始……");
        // 消息数据分片以及线程纷发
        shardingAndMsgThread();
        // 时间监控
        timeMonitor();
    }

    /**
     * 准备推送
     */
    private void preparePushRun() {
        BoostForm boostForm = BoostForm.getInstance();

        // 按钮状态
        boostForm.getScheduledRunButton().setEnabled(false);
        boostForm.getStartButton().setEnabled(false);
        boostForm.getStopButton().setEnabled(true);

        boostForm.getStopButton().setText("停止");
        // 初始化
        boostForm.getProcessedCountLabel().setText("0");
        boostForm.getSuccessCountLabel().setText("0");
        boostForm.getFailCountLabel().setText("0");

        // 设置是否空跑
        PushControl.dryRun = boostForm.getDryRunCheckBox().isSelected();

        // 执行前重新导入目标用户
        PushControl.reimportMembers();

        // 重置推送数据
        PushData.reset();
        PushData.startTime = System.currentTimeMillis();

        // 拷贝准备的目标用户
        PushData.toSendList.addAll(PushData.allUser);
        PushData.TO_SEND_COUNT.set(PushData.allUser.size());
        // 总记录数
        PushData.totalRecords = PushData.toSendList.size();

        boostForm.getMemberCountLabel().setText("消息总数：" + PushData.totalRecords);
        boostForm.getProcessedProgressBar().setMaximum((int) PushData.totalRecords);
        boostForm.getCompletedProgressBar().setMaximum((int) PushData.totalRecords);
        ConsoleUtil.boostConsoleWithLog("消息总数：" + PushData.totalRecords);
        // 可用处理器核心数量
        boostForm.getProcessorCountLabel().setText("可用处理器核心：" + Runtime.getRuntime().availableProcessors());
        ConsoleUtil.boostConsoleWithLog("可用处理器核心：" + Runtime.getRuntime().availableProcessors());

        // 准备消息构造器
        PushControl.prepareMsgMaker();
    }

    /**
     * 消息数据分片以及线程纷发
     */
    private static void shardingAndMsgThread() {

        MsgAsyncSendThread msgAsyncSendThread;

        IMsgSender msgSender = MsgSenderFactory.getMsgSender();
        msgAsyncSendThread = new MsgAsyncSendThread(msgSender);

        ThreadUtil.execute(msgAsyncSendThread);
        ConsoleUtil.boostConsoleWithLog("线程启动完毕……");
    }

    /**
     * 时间监控
     */
    private void timeMonitor() {
        BoostForm boostForm = BoostForm.getInstance();

        long startTimeMillis = System.currentTimeMillis();
        // 计时
        while (true) {
            if (PushData.TO_SEND_COUNT.get() <= PushData.successRecords.longValue() + PushData.failRecords.longValue()) {
                if (!PushData.fixRateScheduling) {
                    boostForm.getStopButton().setEnabled(false);
                    boostForm.getStopButton().updateUI();
                    boostForm.getStartButton().setEnabled(true);
                    boostForm.getStartButton().updateUI();
                    boostForm.getScheduledRunButton().setEnabled(true);
                    boostForm.getScheduledRunButton().updateUI();
                    boostForm.getScheduledTaskLabel().setText("");
                    String finishTip = "发送完毕！\n";
                    JOptionPane.showMessageDialog(boostForm.getBoostPanel(), finishTip, "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    boostForm.getScheduledTaskLabel().setVisible(false);
                } else {
                    if (App.config.isRadioCron()) {
                        Date nextDate = CronPatternUtil.nextDateAfter(new CronPattern(App.config.getTextCron()), new Date(), true);
                        boostForm.getScheduledTaskLabel().setText("计划任务执行中，下一次执行时间：" + DateFormatUtils.format(nextDate, "yyyy-MM-dd HH:mm:ss"));
                    }
                    boostForm.getStopButton().setText("停止计划任务");
                }

                PushData.endTime = System.currentTimeMillis();

                // 保存停止前的数据
                try {
                    // 空跑控制
                    if (!boostForm.getDryRunCheckBox().isSelected()) {
                        ConsoleUtil.boostConsoleWithLog("正在保存结果数据……");
                        boostForm.getCompletedProgressBar().setIndeterminate(true);
                        PushControl.savePushData();
                        ConsoleUtil.boostConsoleWithLog("结果数据保存完毕！");
                    }
                } catch (IOException e) {
                    logger.error(e);
                } finally {
                    boostForm.getCompletedProgressBar().setIndeterminate(false);
                }
                break;
            }

            long currentTimeMillis = System.currentTimeMillis();
            long lastTimeMillis = currentTimeMillis - startTimeMillis;
            long leftTimeMillis = (long) ((double) lastTimeMillis / (PushData.sendSuccessList.size() + PushData.sendFailList.size()) * (PushData.allUser.size() - PushData.sendSuccessList.size() - PushData.sendFailList.size()));

            // 耗时
            String formatBetweenLast = DateUtil.formatBetween(lastTimeMillis, BetweenFormater.Level.SECOND);
            boostForm.getLastTimeLabel().setText("".equals(formatBetweenLast) ? "0s" : formatBetweenLast);

            // 预计剩余
            String formatBetweenLeft = DateUtil.formatBetween(leftTimeMillis, BetweenFormater.Level.SECOND);
            boostForm.getLeftTimeLabel().setText("".equals(formatBetweenLeft) ? "0s" : formatBetweenLeft);

            boostForm.getJvmMemoryLabel().setText("JVM内存占用：" + FileUtil.readableFileSize(Runtime.getRuntime().totalMemory()) + "/" + FileUtil.readableFileSize(Runtime.getRuntime().maxMemory()));

            ThreadUtil.safeSleep(100);
        }
    }

}