package com.fangxuele.tool.wechat.push.ui.listener;

import com.fangxuele.tool.wechat.push.logic.PushData;
import com.fangxuele.tool.wechat.push.logic.RunPushThread;
import com.fangxuele.tool.wechat.push.ui.Init;
import com.fangxuele.tool.wechat.push.ui.MainWindow;
import com.xiaoleilu.hutool.date.DateUtil;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.LogFactory;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 推送tab相关事件监听
 * Created by zhouy on 2017/6/19.
 */
public class PushListener {
    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        // 开始按钮事件
        MainWindow.mainWindow.getPushStartButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (checkBeforePush()) {
                            int isPush = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getPushPanel(),
                                    new StringBuilder("确定开始推送吗？\n\n推送消息：").
                                            append(MainWindow.mainWindow.getMsgNameField().getText()).
                                            append("\n推送人数：").append(PushData.allUser.size()).
                                            append("\n\n空跑模式：").
                                            append(MainWindow.mainWindow.getDryRunCheckBox().isSelected()).toString(), "确认推送？",
                                    JOptionPane.INFORMATION_MESSAGE);
                            if (isPush == JOptionPane.YES_OPTION) {
                                // 按钮状态
                                MainWindow.mainWindow.getScheduleRunButton().setEnabled(false);
                                MainWindow.mainWindow.getPushStartButton().setEnabled(false);
                                MainWindow.mainWindow.getPushStopButton().setEnabled(true);
                                new RunPushThread().start();
                            }
                        }
                    }
                }).start();
            }
        });

        // 停止按钮事件
        MainWindow.mainWindow.getPushStopButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int isStop = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getPushPanel(),
                        "确定停止当前的推送吗？", "确认停止？",
                        JOptionPane.INFORMATION_MESSAGE);
                if (isStop == JOptionPane.YES_OPTION) {
                    PushData.running = false;
                }
            }
        });

        // 按计划执行按钮事件
        MainWindow.mainWindow.getScheduleRunButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (checkBeforePush()) {

                            // 看是否存在设置的计划任务
                            boolean existScheduleTask = false;

                            // 定时开始
                            if (Init.configer.isRadioStartAt()) {
                                long startAtMills = DateUtil.parse(Init.configer.getTextStartAt(), "yyyy-MM-dd HH:mm:ss").getTime();
                                if (startAtMills < System.currentTimeMillis()) {
                                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getPushPanel(), "计划开始推送时间不能小于系统当前时间！\n\n请检查计划任务设置！\n\n", "提示",
                                            JOptionPane.INFORMATION_MESSAGE);
                                    return;
                                }

                                int isSchedulePush = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getPushPanel(),
                                        new StringBuilder("将在").
                                                append(Init.configer.getTextStartAt()).
                                                append("推送消息：\n\n").
                                                append(MainWindow.mainWindow.getMsgNameField().getText()).
                                                append("\n\n推送人数：").append(PushData.allUser.size()).
                                                append("\n\n空跑模式：").
                                                append(MainWindow.mainWindow.getDryRunCheckBox().isSelected()).toString(), "确认定时推送？",
                                        JOptionPane.INFORMATION_MESSAGE);
                                if (isSchedulePush == JOptionPane.YES_OPTION) {
                                    PushData.scheduling = true;
                                    // 按钮状态
                                    MainWindow.mainWindow.getScheduleRunButton().setEnabled(false);
                                    MainWindow.mainWindow.getPushStartButton().setEnabled(false);
                                    MainWindow.mainWindow.getPushStopButton().setEnabled(true);
                                    
                                    ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
                                    service.schedule(new RunPushThread(), startAtMills - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                                }
                                existScheduleTask = true;
                            }

                            // 定时停止
                            if (Init.configer.isRadioStopAt()) {
                                long stopAtMills = DateUtil.parse(Init.configer.getTextStopAt(), "yyyy-MM-dd HH:mm:ss").getTime();
                                if (stopAtMills < System.currentTimeMillis()) {
                                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getPushPanel(), "计划停止推送时间不能小于系统当前时间！\n\n请检查计划任务设置！\n\n", "提示",
                                            JOptionPane.INFORMATION_MESSAGE);
                                    return;
                                }
                                int isScheduleStop = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getPushPanel(),
                                        new StringBuilder("确定开始推送吗？\n\n推送消息：").
                                                append(MainWindow.mainWindow.getMsgNameField().getText()).
                                                append("\n推送人数：").append(PushData.allUser.size()).
                                                append("\n\n空跑模式：").
                                                append(MainWindow.mainWindow.getDryRunCheckBox().isSelected()).toString(), "确认推送？",
                                        JOptionPane.INFORMATION_MESSAGE);
                                if (isScheduleStop == JOptionPane.YES_OPTION) {
                                    PushData.scheduling = true;
                                    // 按钮状态
                                    MainWindow.mainWindow.getScheduleRunButton().setEnabled(false);
                                    MainWindow.mainWindow.getPushStartButton().setEnabled(false);
                                    MainWindow.mainWindow.getPushStopButton().setEnabled(true);

                                    ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
                                    service.schedule(new Runnable() {
                                        @Override
                                        public void run() {
                                            PushData.running = false;
                                        }
                                    }, stopAtMills - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                                }
                                existScheduleTask = true;
                            }

                            // 每天固定时间开始
                            if (Init.configer.isRadioPerDay()) {

                            }

                            // 每周固定时间开始
                            if (Init.configer.isRadioPerWeek()) {

                            }

                            if (!existScheduleTask) {
                                JOptionPane.showMessageDialog(MainWindow.mainWindow.getPushPanel(), "请先设置计划任务！", "提示",
                                        JOptionPane.INFORMATION_MESSAGE);
                            }
                        }
                    }
                }).start();
            }
        });
    }

    /**
     * 推送前检查
     *
     * @return
     */
    private static boolean checkBeforePush() {
        if (PushData.allUser == null || PushData.allUser.size() == 0) {
            JOptionPane.showMessageDialog(MainWindow.mainWindow.getPushPanel(), "请先准备目标用户！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);

            return false;
        }
        if ("0".equals(MainWindow.mainWindow.getPushPageSizeTextField().getText()) || StringUtils.isEmpty(MainWindow.mainWindow.getPushPageSizeTextField().getText())) {
            JOptionPane.showMessageDialog(MainWindow.mainWindow.getPushPanel(), "请设置每页分配用户数！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);

            return false;
        }
        if ("0".equals(MainWindow.mainWindow.getPushPagePerThreadTextField().getText()) || StringUtils.isEmpty(MainWindow.mainWindow.getPushPagePerThreadTextField().getText())) {
            JOptionPane.showMessageDialog(MainWindow.mainWindow.getPushPanel(), "请设置每个线程分配的页数！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);

            return false;
        }
        if (StringUtils.isEmpty(MainWindow.mainWindow.getMsgNameField().getText())) {
            JOptionPane.showMessageDialog(MainWindow.mainWindow.getPushPanel(), "请先编辑消息！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);

            return false;
        }

        return true;
    }

}
