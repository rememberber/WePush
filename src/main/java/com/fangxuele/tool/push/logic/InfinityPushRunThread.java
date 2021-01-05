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
import com.fangxuele.tool.push.logic.msgthread.MsgInfinitySendThread;
import com.fangxuele.tool.push.ui.form.InfinityForm;
import com.fangxuele.tool.push.util.ConsoleUtil;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import java.io.IOException;
import java.util.Date;

/**
 * <pre>
 * 性能模式推送执行控制线程
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/7/7.
 */
public class InfinityPushRunThread extends Thread {

    private static final Log logger = LogFactory.get();

    @Override
    public void run() {
        InfinityForm infinityForm = InfinityForm.getInstance();
        infinityForm.getPushTotalProgressBar().setIndeterminate(true);
        // 准备推送
        preparePushRun();
        infinityForm.getPushTotalProgressBar().setIndeterminate(false);
        ConsoleUtil.infinityConsoleWithLog("推送开始……");
        // 消息数据分片以及线程纷发
        shardingAndMsgThread();
        // 时间监控
        timeMonitor();
    }

    /**
     * 准备推送
     */
    private void preparePushRun() {
        InfinityForm infinityForm = InfinityForm.getInstance();

        // 按钮状态
        infinityForm.getScheduleRunButton().setEnabled(false);
        infinityForm.getPushStartButton().setEnabled(false);
        infinityForm.getPushStopButton().setEnabled(true);

        infinityForm.getPushStopButton().setText("停止");
        // 初始化
        infinityForm.getPushSuccessCount().setText("0");
        infinityForm.getPushFailCount().setText("0");

        // 设置是否空跑
        PushControl.dryRun = infinityForm.getDryRunCheckBox().isSelected();

        // 执行前重新导入目标用户
        PushControl.reimportMembers();

        // 重置推送数据
        PushData.reset();
        PushData.startTime = System.currentTimeMillis();

        // 拷贝准备的目标用户
        PushData.toSendConcurrentLinkedQueue.addAll(PushData.allUser);
        PushData.TO_SEND_COUNT.set(PushData.allUser.size());
        // 总记录数
        PushData.totalRecords = PushData.allUser.size();

        infinityForm.getPushTotalCountLabel().setText("消息总数：" + PushData.totalRecords);
        infinityForm.getPushTotalProgressBar().setMaximum((int) PushData.totalRecords);
        ConsoleUtil.infinityConsoleWithLog("消息总数：" + PushData.totalRecords);
        // 可用处理器核心数量
        infinityForm.getAvailableProcessorLabel().setText("可用处理器核心：" + Runtime.getRuntime().availableProcessors());
        ConsoleUtil.infinityConsoleWithLog("可用处理器核心：" + Runtime.getRuntime().availableProcessors());

        // 准备消息构造器
        PushControl.prepareMsgMaker();
    }

    /**
     * 消息数据分片以及线程纷发
     */
    private static void shardingAndMsgThread() {

        MsgInfinitySendThread msgInfinitySendThread;

        IMsgSender msgSender = MsgSenderFactory.getMsgSender();
        msgInfinitySendThread = new MsgInfinitySendThread(msgSender);

        ThreadUtil.execute(msgInfinitySendThread);
        ConsoleUtil.infinityConsoleWithLog("线程启动完毕……");
    }

    /**
     * 时间监控
     */
    private void timeMonitor() {
        InfinityForm infinityForm = InfinityForm.getInstance();

        long startTimeMillis = System.currentTimeMillis();
        // 计时
        while (true) {
            if (PushData.TO_SEND_COUNT.get() <= PushData.processedRecords.longValue()) {
                if (!PushData.fixRateScheduling) {
                    infinityForm.getPushStopButton().setEnabled(false);
                    infinityForm.getPushStopButton().updateUI();
                    infinityForm.getPushStartButton().setEnabled(true);
                    infinityForm.getPushStartButton().updateUI();
                    infinityForm.getScheduleRunButton().setEnabled(true);
                    infinityForm.getScheduleRunButton().updateUI();
                    infinityForm.getScheduleDetailLabel().setText("");
                    String finishTip = "发送完毕！\n";
                    JOptionPane.showMessageDialog(infinityForm.getInfinityPanel(), finishTip, "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    infinityForm.getScheduleDetailLabel().setVisible(false);
                } else {
                    if (App.config.isRadioCron()) {
                        Date nextDate = CronPatternUtil.nextDateAfter(new CronPattern(App.config.getTextCron()), new Date(), true);
                        infinityForm.getScheduleDetailLabel().setText("计划任务执行中，下一次执行时间：" + DateFormatUtils.format(nextDate, "yyyy-MM-dd HH:mm:ss"));
                    }
                    infinityForm.getPushStopButton().setText("停止计划任务");
                }

                PushData.endTime = System.currentTimeMillis();

                // 保存停止前的数据
                try {
                    // 空跑控制
                    if (!infinityForm.getDryRunCheckBox().isSelected()) {
                        ConsoleUtil.infinityConsoleWithLog("正在保存结果数据……");
                        infinityForm.getPushTotalProgressBar().setIndeterminate(true);
                        PushControl.savePushData();
                        ConsoleUtil.infinityConsoleWithLog("结果数据保存完毕！");
                    }
                } catch (IOException e) {
                    logger.error(e);
                } finally {
                    infinityForm.getPushTotalProgressBar().setIndeterminate(false);
                }
                break;
            }

            long currentTimeMillis = System.currentTimeMillis();
            long lastTimeMillis = currentTimeMillis - startTimeMillis;
            long leftTimeMillis = (long) ((double) lastTimeMillis / PushData.processedRecords.longValue() * (PushData.allUser.size() - PushData.processedRecords.longValue()));

            // 耗时
            String formatBetweenLast = DateUtil.formatBetween(lastTimeMillis, BetweenFormater.Level.SECOND);
            infinityForm.getPushLastTimeLabel().setText("".equals(formatBetweenLast) ? "0s" : formatBetweenLast);

            // 预计剩余
            String formatBetweenLeft = DateUtil.formatBetween(leftTimeMillis, BetweenFormater.Level.SECOND);
            infinityForm.getPushLeftTimeLabel().setText("".equals(formatBetweenLeft) ? "0s" : formatBetweenLeft);

            infinityForm.getJvmMemoryLabel().setText("JVM内存占用：" + FileUtil.readableFileSize(Runtime.getRuntime().totalMemory()) + "/" + FileUtil.readableFileSize(Runtime.getRuntime().maxMemory()));

            ThreadUtil.safeSleep(100);
        }
    }

}