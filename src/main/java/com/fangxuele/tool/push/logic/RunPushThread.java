package com.fangxuele.tool.push.logic;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.hutool.core.date.BetweenFormater;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.component.TableInCellProgressBarRenderer;
import com.fangxuele.tool.push.ui.form.MainWindow;
import me.chanjar.weixin.mp.api.WxMpService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <pre>
 * 推送执行控制线程
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/28.
 */
public class RunPushThread extends Thread {

    private static final Log logger = LogFactory.get();

    @Override
    public void run() {
        MainWindow.mainWindow.getPushStopButton().setText("停止");

        // 初始化
        MainWindow.mainWindow.getPushTotalProgressBar().setIndeterminate(true);
        PushData.running = true;
        PushData.successRecords.reset();
        PushData.failRecords.reset();
        PushData.stopedThreadCount.reset();
        PushData.threadCount = 0;

        MainWindow.mainWindow.getPushSuccessCount().setText("0");
        MainWindow.mainWindow.getPushFailCount().setText("0");

        PushData.toSendList = Collections.synchronizedList(new LinkedList<>());
        PushData.sendSuccessList = Collections.synchronizedList(new LinkedList<>());
        PushData.sendFailList = Collections.synchronizedList(new LinkedList<>());

        PushManage.console("推送开始……");

        // 拷贝准备的目标用户
        PushData.toSendList.addAll(PushData.allUser);
        // 总记录数
        long totalCount = PushData.toSendList.size();
        PushData.totalRecords = totalCount;

        MainWindow.mainWindow.getPushTotalCountLabel().setText("消息总数：" + totalCount);
        MainWindow.mainWindow.getPushTotalProgressBar().setMaximum((int) totalCount);
        PushManage.console("消息总数：" + totalCount);
        // 可用处理器核心数量
        MainWindow.mainWindow.getAvailableProcessorLabel().setText("可用处理器核心：" + Runtime.getRuntime().availableProcessors());
        PushManage.console("可用处理器核心：" + Runtime.getRuntime().availableProcessors());

        // 线程数
        Init.configer.setThreadCount(Integer.parseInt(MainWindow.mainWindow.getThreadCountTextField().getText()));
        Init.configer.save();
        PushManage.console("线程数：" + MainWindow.mainWindow.getThreadCountTextField().getText());

        // 线程池大小
        Init.configer.setMaxThreadPool(Integer.parseInt(MainWindow.mainWindow.getMaxThreadPoolTextField().getText()));
        Init.configer.save();
        PushManage.console("线程池大小：" + MainWindow.mainWindow.getMaxThreadPoolTextField().getText());

        // JVM内存占用
        MainWindow.mainWindow.getJvmMemoryLabel().setText("JVM内存占用：" + Runtime.getRuntime().maxMemory() + "/" + Runtime.getRuntime().totalMemory());
        // 线程数
        int threadCount = Integer.parseInt(MainWindow.mainWindow.getThreadCountTextField().getText());
        PushData.threadCount = threadCount;
        PushManage.console("需要：" + MainWindow.mainWindow.getThreadCountTextField().getText() + "个线程宝宝齐力合作");

        // 初始化线程table
        String[] headerNames = {"线程", "分片区间", "成功", "失败", "总数", "当前进度"};
        DefaultTableModel tableModel = new DefaultTableModel(null, headerNames);
        MainWindow.mainWindow.getPushThreadTable().setModel(tableModel);
        MainWindow.mainWindow.getPushThreadTable().getColumn("当前进度").setCellRenderer(new TableInCellProgressBarRenderer());

        DefaultTableCellRenderer hr = (DefaultTableCellRenderer) MainWindow.mainWindow.getPushThreadTable().getTableHeader()
                .getDefaultRenderer();
        // 表头列名居中
        hr.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
        MainWindow.mainWindow.getPushThreadTable().updateUI();

        Object[] data;
        String msgType = Objects.requireNonNull(MainWindow.mainWindow.getMsgTypeComboBox().getSelectedItem()).toString();

        int maxThreadPoolSize = Integer.parseInt(MainWindow.mainWindow.getMaxThreadPoolTextField().getText());
        ThreadPoolExecutor threadPoolExecutor = ThreadUtil.newExecutor(maxThreadPoolSize, maxThreadPoolSize);
        BaseMsgServiceThread thread = null;
        // 每个线程分配
        int perThread = (int) (totalCount / threadCount) + 1;
        for (int i = 0; i < threadCount; i++) {
            int startIndex = i * perThread;
            int endIndex = i * perThread + perThread;
            if (endIndex > totalCount - 1) {
                endIndex = (int) (totalCount - 1);
            }
            if (MessageTypeConsts.MP_TEMPLATE.equals(msgType)) {
                thread = new TemplateMsgMpServiceThread(startIndex, endIndex);

                WxMpService wxMpService = PushManage.getWxMpService();
                if (wxMpService.getWxMpConfigStorage() == null) {
                    return;
                }
                thread.setWxMpService(wxMpService);
            } else if (MessageTypeConsts.MA_TEMPLATE.equals(msgType)) {
                thread = new TemplateMsgMaServiceThread(startIndex, endIndex);

                WxMaService wxMaService = PushManage.getWxMaService();
                if (wxMaService.getWxMaConfig() == null) {
                    return;
                }
                ((TemplateMsgMaServiceThread) thread).setWxMaService(wxMaService);
            } else if (MessageTypeConsts.KEFU.equals(msgType)) {
                thread = new KeFuMsgServiceThread(startIndex, endIndex);

                WxMpService wxMpService = PushManage.getWxMpService();
                if (wxMpService.getWxMpConfigStorage() == null) {
                    return;
                }
                thread.setWxMpService(wxMpService);
            } else if (MessageTypeConsts.KEFU_PRIORITY.equals(msgType)) {
                thread = new KeFuPriorMsgServiceThread(startIndex, endIndex);

                WxMpService wxMpService = PushManage.getWxMpService();
                if (wxMpService.getWxMpConfigStorage() == null) {
                    return;
                }
                thread.setWxMpService(wxMpService);
            } else if (MessageTypeConsts.ALI_TEMPLATE.equals(msgType)) {
                thread = new AliDayuTemplateSmsMsgServiceThread(startIndex, endIndex);
            } else if (MessageTypeConsts.ALI_YUN.equals(msgType)) {
                thread = new AliYunSmsMsgServiceThread(startIndex, endIndex);
            } else if (MessageTypeConsts.TX_YUN.equals(msgType)) {
                thread = new TxYunSmsMsgServiceThread(startIndex, endIndex);
            } else if (MessageTypeConsts.YUN_PIAN.equals(msgType)) {
                thread = new YunpianSmsMsgServiceThread(startIndex, endIndex);
            }

            thread.setTableRow(i);
            thread.setName("T-" + i);

            data = new Object[6];
            data[0] = thread.getName();
            data[1] = startIndex + "-" + endIndex;
            data[5] = 0;
            tableModel.addRow(data);

            threadPoolExecutor.execute(thread);
        }
        MainWindow.mainWindow.getPushTotalProgressBar().setIndeterminate(false);
        PushManage.console("所有线程宝宝启动完毕……");

        long startTimeMillis = System.currentTimeMillis();
        // 计时
        while (true) {
            if (PushData.stopedThreadCount.intValue() == threadCount) {
                if (!PushData.fixRateScheduling) {
                    MainWindow.mainWindow.getPushStopButton().setEnabled(false);
                    MainWindow.mainWindow.getPushStopButton().updateUI();
                }

                String finishTip = "发送完毕！\n\n";
                if (!MainWindow.mainWindow.getDryRunCheckBox().isSelected()) {
                    finishTip = "发送完毕！\n\n接下来将保存结果数据，请等待……\n\n";
                }
                if (!PushData.fixRateScheduling) {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getPushPanel(), finishTip, "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }

                // 保存停止前的数据
                try {
                    PushManage.console("正在保存结果数据……");
                    MainWindow.mainWindow.getPushTotalProgressBar().setIndeterminate(true);
                    // 空跑控制
                    if (!MainWindow.mainWindow.getDryRunCheckBox().isSelected()) {
                        PushManage.savePushData();
                    }
                    PushManage.console("结果数据保存完毕！");
                } catch (IOException e) {
                    logger.error(e);
                } finally {
                    MainWindow.mainWindow.getPushTotalProgressBar().setIndeterminate(false);
                }

                if (!PushData.fixRateScheduling) {
                    MainWindow.mainWindow.getPushStartButton().setEnabled(true);
                    MainWindow.mainWindow.getScheduleRunButton().setEnabled(true);
                    MainWindow.mainWindow.getPushStartButton().updateUI();
                    MainWindow.mainWindow.getScheduleRunButton().updateUI();

                    MainWindow.mainWindow.getScheduleDetailLabel().setText("");
                } else {
                    MainWindow.mainWindow.getPushStopButton().setText("停止计划任务");
                }

                break;
            }

            long currentTimeMillis = System.currentTimeMillis();
            long lastTimeMillis = currentTimeMillis - startTimeMillis;
            long leftTimeMillis = (long) ((double) lastTimeMillis / (PushData.sendSuccessList.size() + PushData.sendFailList.size()) * (PushData.allUser.size() - PushData.sendSuccessList.size() - PushData.sendFailList.size()));

            String formatBetweenLast = DateUtil.formatBetween(lastTimeMillis, BetweenFormater.Level.SECOND);
            MainWindow.mainWindow.getPushLastTimeLabel().setText("".equals(formatBetweenLast) ? "0s" : formatBetweenLast);

            String formatBetweenLeft = DateUtil.formatBetween(leftTimeMillis, BetweenFormater.Level.SECOND);
            MainWindow.mainWindow.getPushLeftTimeLabel().setText("".equals(formatBetweenLeft) ? "0s" : formatBetweenLeft);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                logger.error(e);
            }
        }
    }

}