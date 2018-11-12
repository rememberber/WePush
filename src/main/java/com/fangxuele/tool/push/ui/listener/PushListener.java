package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.logic.RunPushThread;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.MainWindow;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 推送tab相关事件监听
 * Created by rememberber(https://github.com/rememberber) on 2017/6/19.
 */
public class PushListener {
    private static final Log logger = LogFactory.get();

    public static ScheduledExecutorService serviceStartAt;

    public static ScheduledExecutorService serviceStartPerDay;

    public static ScheduledExecutorService serviceStartPerWeek;

    public static void addListeners() {
        // 开始按钮事件
        MainWindow.mainWindow.getPushStartButton().addActionListener((e) -> new Thread(() -> {
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
        }).start());

        // 停止按钮事件
        MainWindow.mainWindow.getPushStopButton().addActionListener((e) -> {
            if (!PushData.running && PushData.scheduling) {
                MainWindow.mainWindow.getScheduleDetailLabel().setText("");
                if (serviceStartAt != null) {
                    serviceStartAt.shutdownNow();
                }
                MainWindow.mainWindow.getPushStartButton().setEnabled(true);
                MainWindow.mainWindow.getScheduleRunButton().setEnabled(true);
                MainWindow.mainWindow.getPushStopButton().setText("停止");
                MainWindow.mainWindow.getPushStopButton().setEnabled(false);
                MainWindow.mainWindow.getPushStartButton().updateUI();
                MainWindow.mainWindow.getScheduleRunButton().updateUI();
                MainWindow.mainWindow.getPushStopButton().updateUI();
                PushData.scheduling = false;
            }

            if (!PushData.running && PushData.fixRateScheduling) {
                MainWindow.mainWindow.getScheduleDetailLabel().setText("");
                if (serviceStartPerDay != null) {
                    serviceStartPerDay.shutdownNow();
                }
                if (serviceStartPerWeek != null) {
                    serviceStartPerWeek.shutdownNow();
                }
                MainWindow.mainWindow.getPushStartButton().setEnabled(true);
                MainWindow.mainWindow.getScheduleRunButton().setEnabled(true);
                MainWindow.mainWindow.getPushStopButton().setText("停止");
                MainWindow.mainWindow.getPushStopButton().setEnabled(false);
                MainWindow.mainWindow.getPushStartButton().updateUI();
                MainWindow.mainWindow.getScheduleRunButton().updateUI();
                MainWindow.mainWindow.getPushStopButton().updateUI();
                PushData.fixRateScheduling = false;
            }

            if (PushData.running) {
                int isStop = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getPushPanel(),
                        "确定停止当前的推送吗？", "确认停止？",
                        JOptionPane.INFORMATION_MESSAGE);
                if (isStop == JOptionPane.YES_OPTION) {
                    PushData.running = false;
                }
            }
        });

        // 按计划执行按钮事件
        MainWindow.mainWindow.getScheduleRunButton().addActionListener((e -> new Thread(() -> {
            if (checkBeforePush()) {

                // 看是否存在设置的计划任务
                boolean existScheduleTask = false;

                // 定时开始
                if (Init.configer.isRadioStartAt()) {
                    long startAtMills = DateUtil.parse(Init.configer.getTextStartAt(), DatePattern.NORM_DATETIME_PATTERN).getTime();
                    if (startAtMills < System.currentTimeMillis()) {
                        JOptionPane.showMessageDialog(MainWindow.mainWindow.getPushPanel(), "计划开始推送时间不能小于系统当前时间！\n\n请检查计划任务设置！\n\n", "提示",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    int isSchedulePush = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getPushPanel(),
                            new StringBuilder("将在").
                                    append(Init.configer.getTextStartAt()).
                                    append("推送\n\n消息：").
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
                        MainWindow.mainWindow.getPushStopButton().setText("停止计划任务");
                        MainWindow.mainWindow.getPushStopButton().setEnabled(true);

                        MainWindow.mainWindow.getScheduleDetailLabel().setText(new StringBuilder("计划任务执行中：将在").
                                append(Init.configer.getTextStartAt()).
                                append("开始推送").toString());

                        serviceStartAt = Executors.newSingleThreadScheduledExecutor();
                        serviceStartAt.schedule(new RunPushThread(), startAtMills - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                    }
                    existScheduleTask = true;
                }

                // 每天固定时间开始
                if (Init.configer.isRadioPerDay()) {
                    long startPerDayMills = DateUtil.parse(DateUtil.today() + " " + Init.configer.getTextPerDay(), DatePattern.NORM_DATETIME_PATTERN).getTime();

                    int isSchedulePush = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getPushPanel(),
                            new StringBuilder("将在每天").
                                    append(Init.configer.getTextPerDay()).
                                    append("推送\n\n消息：").
                                    append(MainWindow.mainWindow.getMsgNameField().getText()).
                                    append("\n\n推送人数：").append(PushData.allUser.size()).
                                    append("\n\n空跑模式：").
                                    append(MainWindow.mainWindow.getDryRunCheckBox().isSelected()).toString(), "确认定时推送？",
                            JOptionPane.INFORMATION_MESSAGE);
                    if (isSchedulePush == JOptionPane.YES_OPTION) {
                        PushData.fixRateScheduling = true;
                        // 按钮状态
                        MainWindow.mainWindow.getScheduleRunButton().setEnabled(false);
                        MainWindow.mainWindow.getPushStartButton().setEnabled(false);
                        MainWindow.mainWindow.getPushStopButton().setText("停止计划任务");
                        MainWindow.mainWindow.getPushStopButton().setEnabled(true);

                        MainWindow.mainWindow.getScheduleDetailLabel().setText(new StringBuilder("计划任务执行中：将在每天").
                                append(Init.configer.getTextPerDay()).
                                append("开始推送").toString());

                        serviceStartPerDay = Executors.newSingleThreadScheduledExecutor();
                        long millisBetween = startPerDayMills - System.currentTimeMillis();
                        long delay = millisBetween < 0 ? millisBetween + 24 * 60 * 60 * 1000 : millisBetween;
                        serviceStartPerDay.scheduleAtFixedRate(new RunPushThread(), delay, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
                    }
                    existScheduleTask = true;
                }

                // 每周固定时间开始
                if (Init.configer.isRadioPerWeek()) {

                    long todaySetMills = DateUtil.parse(DateUtil.today() + " " + Init.configer.getTextPerWeekTime(), DatePattern.NORM_DATETIME_PATTERN).getTime();
                    int dayBetween = getDayOfWeek(Init.configer.getTextPerWeekWeek()) - DateUtil.thisDayOfWeek();
                    long startPerWeekMills = dayBetween < 0 ? (dayBetween + 7) * 24 * 60 * 60 * 1000 : dayBetween * 24 * 60 * 60 * 1000;

                    int isSchedulePush = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getPushPanel(),
                            new StringBuilder("将在每周").append(Init.configer.getTextPerWeekWeek())
                                    .append(Init.configer.getTextPerWeekTime())
                                    .append("推送\n\n消息：")
                                    .append(MainWindow.mainWindow.getMsgNameField().getText())
                                    .append("\n\n推送人数：").append(PushData.allUser.size())
                                    .append("\n\n空跑模式：")
                                    .append(MainWindow.mainWindow.getDryRunCheckBox().isSelected()).toString(), "确认定时推送？",
                            JOptionPane.INFORMATION_MESSAGE);
                    if (isSchedulePush == JOptionPane.YES_OPTION) {
                        PushData.fixRateScheduling = true;
                        // 按钮状态
                        MainWindow.mainWindow.getScheduleRunButton().setEnabled(false);
                        MainWindow.mainWindow.getPushStartButton().setEnabled(false);
                        MainWindow.mainWindow.getPushStopButton().setText("停止计划任务");
                        MainWindow.mainWindow.getPushStopButton().setEnabled(true);

                        MainWindow.mainWindow.getScheduleDetailLabel().setText(new StringBuilder("计划任务执行中：将在每周")
                                .append(Init.configer.getTextPerWeekWeek())
                                .append(Init.configer.getTextPerWeekTime())
                                .append("开始推送").toString());

                        serviceStartPerWeek = Executors.newSingleThreadScheduledExecutor();
                        long millisBetween = startPerWeekMills + todaySetMills - System.currentTimeMillis();
                        long delay = millisBetween < 0 ? millisBetween + 7 * 24 * 60 * 60 * 1000 : millisBetween;
                        serviceStartPerWeek.scheduleAtFixedRate(new RunPushThread(), delay, 7 * 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
                    }
                    existScheduleTask = true;
                }

                if (!existScheduleTask) {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getPushPanel(), "请先设置计划任务！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }).start()));

        // 每页分配用户数失去焦点
        MainWindow.mainWindow.getPushPageSizeTextField().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    if (PushData.allUser != null && PushData.allUser.size() > 0) {
                        refreshPushInfo();
                    }
                } catch (Exception e1) {
                    logger.error(e1);
                } finally {
                    super.focusLost(e);
                }
            }
        });

        // 每页分配用户数键入回车
        MainWindow.mainWindow.getPushPageSizeTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    try {
                        if (PushData.allUser != null && PushData.allUser.size() > 0) {
                            refreshPushInfo();
                        }
                    } catch (Exception e1) {
                        logger.error(e1);
                    } finally {
                        super.keyPressed(e);
                    }
                }
            }
        });

        // 每个线程分配的页数失去焦点
        MainWindow.mainWindow.getPushPagePerThreadTextField().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    if (PushData.allUser != null && PushData.allUser.size() > 0) {
                        refreshPushInfo();
                    }
                } catch (Exception e1) {
                    logger.error(e1);
                } finally {
                    super.focusLost(e);
                }
            }
        });

        // 每个线程分配的页数键入回车
        MainWindow.mainWindow.getPushPagePerThreadTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    try {
                        if (PushData.allUser != null && PushData.allUser.size() > 0) {
                            refreshPushInfo();
                        }
                    } catch (Exception e1) {
                        logger.error(e1);
                    } finally {
                        super.keyPressed(e);
                    }
                }
            }
        });

    }

    public static void refreshPushInfo() {
        // 页大小
        int pageSize = Integer.parseInt(MainWindow.mainWindow.getPushPageSizeTextField().getText());
        // 总记录数
        long totalCount = PushData.allUser.size();
        MainWindow.mainWindow.getPushTotalCountLabel().setText("总用户数：" + totalCount);
        MainWindow.mainWindow.getPushTotalProgressBar().setMaximum((int) totalCount);
        // 总页数
        int totalPage = Long.valueOf((totalCount + pageSize - 1) / pageSize).intValue();
        MainWindow.mainWindow.getPushTotalPageLabel().setText("总页数：" + totalPage);
        // 每个线程分配多少页
        int pagePerThread = Integer.parseInt(MainWindow.mainWindow.getPushPagePerThreadTextField().getText());
        // 需要多少个线程
        int threadCount = (totalPage + pagePerThread - 1) / pagePerThread;
        MainWindow.mainWindow.getPushTotalThreadLabel().setText("需要线程宝宝个数：" + threadCount);
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

    public static int getDayOfWeek(String week) {
        int dayOfWeek;
        switch (week) {
            case "一":
                dayOfWeek = 2;
                break;
            case "二":
                dayOfWeek = 3;
                break;
            case "三":
                dayOfWeek = 4;
                break;
            case "四":
                dayOfWeek = 5;
                break;
            case "五":
                dayOfWeek = 6;
                break;
            case "六":
                dayOfWeek = 7;
                break;
            case "日":
                dayOfWeek = 1;
                break;
            default:
                dayOfWeek = 0;
        }
        return dayOfWeek;
    }

}
