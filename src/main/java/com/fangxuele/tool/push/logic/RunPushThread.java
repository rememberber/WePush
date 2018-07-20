package com.fangxuele.tool.push.logic;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.hutool.core.date.BetweenFormater;
import cn.hutool.core.date.DateUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.MainWindow;
import me.chanjar.weixin.mp.api.WxMpService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;

/**
 * 推送执行控制线程
 * Created by rememberber(https://github.com/rememberber) on 2017/6/28.
 */
public class RunPushThread extends Thread {

    private static final Log logger = LogFactory.get();

    @Override
    public void run() {
        MainWindow.mainWindow.getPushStopButton().setText("停止");

        // 初始化
        MainWindow.mainWindow.getPushTotalProgressBar().setIndeterminate(true);
        PushData.running = true;
        PushData.successRecords = 0;
        PushData.failRecords = 0;
        PushData.stopedThreadCount = 0;
        PushData.threadCount = 0;

        MainWindow.mainWindow.getPushSuccessCount().setText("0");
        MainWindow.mainWindow.getPushFailCount().setText("0");

        PushData.toSendList = Collections.synchronizedList(new LinkedList<>());
        PushData.sendSuccessList = Collections.synchronizedList(new LinkedList<>());
        PushData.sendFailList = Collections.synchronizedList(new LinkedList<>());

        PushManage.console("推送开始……");

        // 页大小
        int pageSize = Integer.parseInt(MainWindow.mainWindow.getPushPageSizeTextField().getText());
        Init.configer.setRecordPerPage(pageSize);
        Init.configer.save();
        PushManage.console("页大小：" + pageSize);

        // 拷贝准备的目标用户
        PushData.toSendList.addAll(PushData.allUser);
        // 总记录数
        long totalCount = PushData.toSendList.size();
        PushData.totalRecords = totalCount;

        MainWindow.mainWindow.getPushTotalCountLabel().setText("总用户数：" + totalCount);
        MainWindow.mainWindow.getPushTotalProgressBar().setMaximum((int) totalCount);
        PushManage.console("总用户数：" + totalCount);
        // 总页数
        int totalPage = Long.valueOf((totalCount + pageSize - 1) / pageSize).intValue();
        MainWindow.mainWindow.getPushTotalPageLabel().setText("总页数：" + totalPage);
        PushManage.console("总页数：" + totalPage);

        // 每个线程分配多少页
        int pagePerThread = Integer.parseInt(MainWindow.mainWindow.getPushPagePerThreadTextField().getText());
        Init.configer.setPagePerThread(pagePerThread);
        Init.configer.save();
        PushManage.console(new StringBuffer().append("每个线程分配：").append(pagePerThread).append("页").toString());

        // 需要多少个线程
        int threadCount = (totalPage + pagePerThread - 1) / pagePerThread;
        PushData.threadCount = threadCount;
        MainWindow.mainWindow.getPushTotalThreadLabel().setText("需要线程宝宝个数：" + threadCount);
        PushManage.console(new StringBuffer().append("需要：").append(threadCount).append("个线程宝宝齐力合作").toString());

        // 初始化线程table
        String[] headerNames = {"线程", "页数区间", "成功", "失败", "总数", "当前进度"};
        DefaultTableModel tableModel = new DefaultTableModel(null, headerNames);
        MainWindow.mainWindow.getPushThreadTable().setModel(tableModel);
        MainWindow.mainWindow.getPushThreadTable().getColumn("当前进度").setCellRenderer(new MyProgressBarRenderer());

        DefaultTableCellRenderer hr = (DefaultTableCellRenderer) MainWindow.mainWindow.getPushThreadTable().getTableHeader()
                .getDefaultRenderer();
        // 表头列名居中
        hr.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
        MainWindow.mainWindow.getPushThreadTable().updateUI();

        Object[] data;
        String msgType = MainWindow.mainWindow.getMsgTypeComboBox().getSelectedItem().toString();
        BaseMsgServiceThread thread = null;
        for (int i = 0; i < threadCount; i++) {
            if ("模板消息".equals(msgType)) {
                thread = new TemplateMsgMpServiceThread(i * pagePerThread,
                        i * pagePerThread + pagePerThread - 1, pageSize);

                WxMpService wxMpService = PushManage.getWxMpService();
                if (wxMpService.getWxMpConfigStorage() == null) {
                    return;
                }
                thread.setWxMpService(wxMpService);
            } else if ("模板消息-小程序".equals(msgType)) {
                thread = new TemplateMsgMaServiceThread(i * pagePerThread,
                        i * pagePerThread + pagePerThread - 1, pageSize);

                WxMaService wxMaService = PushManage.getWxMaService();
                if (wxMaService.getWxMaConfig() == null) {
                    return;
                }
                ((TemplateMsgMaServiceThread) thread).setWxMaService(wxMaService);
            } else if ("客服消息".equals(msgType)) {
                thread = new KeFuMsgServiceThread(i * pagePerThread,
                        i * pagePerThread + pagePerThread - 1, pageSize);

                WxMpService wxMpService = PushManage.getWxMpService();
                if (wxMpService.getWxMpConfigStorage() == null) {
                    return;
                }
                thread.setWxMpService(wxMpService);
            } else if ("客服消息优先".equals(msgType)) {
                thread = new KeFuPriorMsgServiceThread(i * pagePerThread,
                        i * pagePerThread + pagePerThread - 1, pageSize);

                WxMpService wxMpService = PushManage.getWxMpService();
                if (wxMpService.getWxMpConfigStorage() == null) {
                    return;
                }
                thread.setWxMpService(wxMpService);
            } else if ("阿里大于模板短信".equals(msgType)) {
                thread = new AliDayuTemplateSmsMsgServiceThread(i * pagePerThread,
                        i * pagePerThread + pagePerThread - 1, pageSize);
            } else if ("阿里云短信".equals(msgType)) {
                thread = new AliYunSmsMsgServiceThread(i * pagePerThread,
                        i * pagePerThread + pagePerThread - 1, pageSize);
            } else if ("腾讯云短信".equals(msgType)) {
                thread = new TxYunSmsMsgServiceThread(i * pagePerThread,
                        i * pagePerThread + pagePerThread - 1, pageSize);
            } else if ("云片网短信".equals(msgType)) {
                thread = new YunpianSmsMsgServiceThread(i * pagePerThread,
                        i * pagePerThread + pagePerThread - 1, pageSize);
            }

            thread.setName(new StringBuffer().append("T-").append(i).toString());

            data = new Object[6];
            data[0] = thread.getName();
            data[1] = i * pagePerThread + "-" + (i * pagePerThread + pagePerThread - 1);
            data[5] = 0;
            tableModel.addRow(data);

            thread.start();
        }
        MainWindow.mainWindow.getPushTotalProgressBar().setIndeterminate(false);
        PushManage.console("所有线程宝宝启动完毕……");

        long startTimeMillis = System.currentTimeMillis();
        // 计时
        while (true) {
            if (PushData.stopedThreadCount == threadCount) {
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

    /**
     * 自定义进度条单元格渲染器
     */
    public static class MyProgressBarRenderer extends JProgressBar implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Integer v = (Integer) value;//这一列必须都是integer类型(0-100)
            setValue(v);
            return this;
        }
    }
}
