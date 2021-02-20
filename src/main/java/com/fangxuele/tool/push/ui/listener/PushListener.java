package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.hutool.cron.pattern.CronPatternUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.logic.PushRunThread;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.dialog.CommonTipsDialog;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.PushForm;
import com.fangxuele.tool.push.ui.form.ScheduleForm;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <pre>
 * 推送tab相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/19.
 */
public class PushListener {
    private static final Log logger = LogFactory.get();

    private static ScheduledExecutorService serviceStartAt;

    private static ScheduledExecutorService serviceStartPerDay;

    private static ScheduledExecutorService serviceStartPerWeek;

    public static void addListeners() {
        PushForm pushForm = PushForm.getInstance();
        JPanel pushPanel = pushForm.getPushPanel();

        // 开始按钮事件
        pushForm.getPushStartButton().addActionListener((e) -> {
            if (PushControl.pushCheck()) {
                if ("0".equals(pushForm.getThreadCountTextField().getText()) || StringUtils.isEmpty(pushForm.getThreadCountTextField().getText())) {
                    JOptionPane.showMessageDialog(pushForm.getPushPanel(), "请设置每个线程分配的页数！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);

                    return;
                }
                int isPush = JOptionPane.showConfirmDialog(pushPanel,
                        "确定开始推送吗？\n\n推送消息：" +
                                MessageEditForm.getInstance().getMsgNameField().getText() +
                                "\n推送人数：" + PushData.allUser.size() +
                                "\n\n空跑模式：" +
                                pushForm.getDryRunCheckBox().isSelected() + "\n", "确认推送？",
                        JOptionPane.YES_NO_OPTION);
                if (isPush == JOptionPane.YES_OPTION) {
                    ThreadUtil.execute(new PushRunThread());
                }
            }
        });

        // 停止按钮事件
        pushForm.getPushStopButton().addActionListener((e) -> {
            ThreadUtil.execute(() -> {
                if (PushData.scheduling) {
                    pushForm.getScheduleDetailLabel().setText("");
                    if (serviceStartAt != null) {
                        serviceStartAt.shutdownNow();
                    }
                    pushForm.getPushStartButton().setEnabled(true);
                    pushForm.getThreadCountSlider().setEnabled(true);
                    pushForm.getScheduleRunButton().setEnabled(true);
                    pushForm.getPushStopButton().setText("停止");
                    pushForm.getPushStopButton().setEnabled(false);
                    pushForm.getPushStartButton().updateUI();
                    pushForm.getScheduleRunButton().updateUI();
                    pushForm.getPushStopButton().updateUI();
                    pushForm.getScheduleDetailLabel().setVisible(false);
                    PushData.scheduling = false;
                    PushData.running = false;
                }

                if (PushData.fixRateScheduling) {
                    pushForm.getScheduleDetailLabel().setText("");
                    if (serviceStartPerDay != null) {
                        serviceStartPerDay.shutdownNow();
                    }
                    if (serviceStartPerWeek != null) {
                        serviceStartPerWeek.shutdownNow();
                    }
                    try {
                        CronUtil.stop();
                    } catch (Exception e1) {
                        logger.warn(e1.toString());
                    }
                    pushForm.getPushStartButton().setEnabled(true);
                    pushForm.getThreadCountSlider().setEnabled(true);
                    pushForm.getScheduleRunButton().setEnabled(true);
                    pushForm.getPushStopButton().setText("停止");
                    pushForm.getPushStopButton().setEnabled(false);
                    pushForm.getPushStartButton().updateUI();
                    pushForm.getScheduleRunButton().updateUI();
                    pushForm.getPushStopButton().updateUI();
                    pushForm.getScheduleDetailLabel().setVisible(false);
                    PushData.fixRateScheduling = false;
                    PushData.running = false;
                }

                if (PushData.running) {
                    int isStop = JOptionPane.showConfirmDialog(pushPanel,
                            "确定停止当前的推送吗？", "确认停止？",
                            JOptionPane.YES_NO_OPTION);
                    if (isStop == JOptionPane.YES_OPTION) {
                        PushData.running = false;
                        pushForm.getPushStartButton().setEnabled(true);
                        pushForm.getThreadCountSlider().setEnabled(true);
                        pushForm.getScheduleRunButton().setEnabled(true);
                        pushForm.getPushStopButton().setText("停止");
                        pushForm.getPushStopButton().setEnabled(false);
                        pushForm.getPushStartButton().updateUI();
                        pushForm.getScheduleRunButton().updateUI();
                        pushForm.getPushStopButton().updateUI();
                        pushForm.getScheduleDetailLabel().setVisible(false);
                    }
                }
            });
        });

        // 按计划执行按钮事件
        pushForm.getScheduleRunButton().addActionListener((e -> ThreadUtil.execute(() -> {
            if (PushControl.pushCheck()) {
                if ("0".equals(pushForm.getThreadCountTextField().getText()) || StringUtils.isEmpty(pushForm.getThreadCountTextField().getText())) {
                    JOptionPane.showMessageDialog(pushForm.getPushPanel(), "请设置每个线程分配的页数！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);

                    return;
                }

                // 看是否存在设置的计划任务
                boolean existScheduleTask = false;

                // 定时开始
                if (App.config.isRadioStartAt()) {
                    long startAtMills = DateUtil.parse(App.config.getTextStartAt(), DatePattern.NORM_DATETIME_PATTERN).getTime();
                    if (startAtMills < System.currentTimeMillis()) {
                        JOptionPane.showMessageDialog(pushPanel, "计划开始推送时间不能小于系统当前时间！\n\n请检查计划任务设置！\n\n", "提示",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    int isSchedulePush = JOptionPane.showConfirmDialog(pushPanel,
                            "将在" +
                                    App.config.getTextStartAt() +
                                    "推送\n\n消息：" +
                                    MessageEditForm.getInstance().getMsgNameField().getText() +
                                    "\n\n推送人数：" + PushData.allUser.size() +
                                    "\n\n空跑模式：" +
                                    pushForm.getDryRunCheckBox().isSelected(), "确认定时推送？",
                            JOptionPane.YES_NO_OPTION);
                    if (isSchedulePush == JOptionPane.YES_OPTION) {
                        PushData.scheduling = true;
                        // 按钮状态
                        pushForm.getScheduleRunButton().setEnabled(false);
                        pushForm.getPushStartButton().setEnabled(false);
                        pushForm.getPushStartButton().setEnabled(false);
                        pushForm.getPushStopButton().setText("停止计划任务");
                        pushForm.getPushStopButton().setEnabled(true);

                        pushForm.getScheduleDetailLabel().setVisible(true);
                        pushForm.getScheduleDetailLabel().setText("计划任务执行中：将在" +
                                App.config.getTextStartAt() +
                                "开始推送");

                        serviceStartAt = Executors.newSingleThreadScheduledExecutor();
                        serviceStartAt.schedule(new PushRunThread(), startAtMills - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                    }
                    existScheduleTask = true;
                }

                // 每天固定时间开始
                if (App.config.isRadioPerDay()) {
                    long startPerDayMills = DateUtil.parse(DateUtil.today() + " " + App.config.getTextPerDay(), DatePattern.NORM_DATETIME_PATTERN).getTime();

                    int isSchedulePush = JOptionPane.showConfirmDialog(pushPanel,
                            "将在每天" +
                                    App.config.getTextPerDay() +
                                    "推送\n\n消息：" +
                                    MessageEditForm.getInstance().getMsgNameField().getText() +
                                    "\n\n推送人数：" + PushData.allUser.size() +
                                    "\n\n空跑模式：" +
                                    pushForm.getDryRunCheckBox().isSelected(), "确认定时推送？",
                            JOptionPane.YES_NO_OPTION);
                    if (isSchedulePush == JOptionPane.YES_OPTION) {
                        PushData.fixRateScheduling = true;
                        // 按钮状态
                        pushForm.getScheduleRunButton().setEnabled(false);
                        pushForm.getPushStartButton().setEnabled(false);
                        pushForm.getPushStopButton().setText("停止计划任务");
                        pushForm.getPushStopButton().setEnabled(true);

                        pushForm.getScheduleDetailLabel().setVisible(true);
                        pushForm.getScheduleDetailLabel().setText("计划任务执行中：将在每天" +
                                App.config.getTextPerDay() +
                                "开始推送");

                        serviceStartPerDay = Executors.newSingleThreadScheduledExecutor();
                        long millisBetween = startPerDayMills - System.currentTimeMillis();
                        long delay = millisBetween < 0 ? millisBetween + 24 * 60 * 60 * 1000 : millisBetween;
                        serviceStartPerDay.scheduleAtFixedRate(new PushRunThread(), delay, 24, TimeUnit.HOURS);
                    }
                    existScheduleTask = true;
                }

                // 每周固定时间开始
                if (App.config.isRadioPerWeek()) {

                    long todaySetMills = DateUtil.parse(DateUtil.today() + " " + App.config.getTextPerWeekTime(), DatePattern.NORM_DATETIME_PATTERN).getTime();
                    int dayBetween = ScheduleForm.getDayOfWeek(App.config.getTextPerWeekWeek()) - DateUtil.thisDayOfWeek();
                    long startPerWeekMills = dayBetween < 0 ? (dayBetween + 7) * 24 * 60 * 60 * 1000 : dayBetween * 24 * 60 * 60 * 1000;

                    int isSchedulePush = JOptionPane.showConfirmDialog(pushPanel,
                            "将在每周" + App.config.getTextPerWeekWeek() +
                                    App.config.getTextPerWeekTime() +
                                    "推送\n\n消息：" +
                                    MessageEditForm.getInstance().getMsgNameField().getText() +
                                    "\n\n推送人数：" + PushData.allUser.size() +
                                    "\n\n空跑模式：" +
                                    pushForm.getDryRunCheckBox().isSelected(), "确认定时推送？",
                            JOptionPane.YES_NO_OPTION);
                    if (isSchedulePush == JOptionPane.YES_OPTION) {
                        PushData.scheduling = true;
                        PushData.fixRateScheduling = true;
                        // 按钮状态
                        pushForm.getScheduleRunButton().setEnabled(false);
                        pushForm.getPushStartButton().setEnabled(false);
                        pushForm.getPushStopButton().setText("停止计划任务");
                        pushForm.getPushStopButton().setEnabled(true);

                        pushForm.getScheduleDetailLabel().setVisible(true);
                        pushForm.getScheduleDetailLabel().setText("计划任务执行中：将在每周" +
                                App.config.getTextPerWeekWeek() +
                                App.config.getTextPerWeekTime() +
                                "开始推送");

                        serviceStartPerWeek = Executors.newSingleThreadScheduledExecutor();
                        long millisBetween = startPerWeekMills + todaySetMills - System.currentTimeMillis();
                        long delay = millisBetween < 0 ? millisBetween + 7 * 24 * 60 * 60 * 1000 : millisBetween;
                        serviceStartPerWeek.scheduleAtFixedRate(new PushRunThread(), delay, 7 * 24, TimeUnit.HOURS);
                    }
                    existScheduleTask = true;
                }

                // 按Cron表达式触发
                if (App.config.isRadioCron()) {

                    List<String> latest5RunTimeList = Lists.newArrayList();
                    Date now = new Date();
                    for (int i = 0; i < 5; i++) {
                        Date date = CronPatternUtil.nextDateAfter(new CronPattern(App.config.getTextCron()), DateUtils.addDays(now, i), true);
                        latest5RunTimeList.add(DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss"));
                    }

                    int isSchedulePush = JOptionPane.showConfirmDialog(pushPanel,
                            "将按" +
                                    App.config.getTextCron() +
                                    "表达式触发推送\n\n" +
                                    "最近5次运行时间:\n" +
                                    String.join("\n", latest5RunTimeList) +
                                    "\n\n消息名称：" +
                                    MessageEditForm.getInstance().getMsgNameField().getText() +
                                    "\n推送人数：" + PushData.allUser.size() +
                                    "\n空跑模式：" +
                                    pushForm.getDryRunCheckBox().isSelected(), "确认定时推送？",
                            JOptionPane.YES_NO_OPTION);
                    if (isSchedulePush == JOptionPane.YES_OPTION) {
                        PushData.fixRateScheduling = true;
                        // 按钮状态
                        pushForm.getScheduleRunButton().setEnabled(false);
                        pushForm.getPushStartButton().setEnabled(false);
                        pushForm.getPushStopButton().setText("停止计划任务");
                        pushForm.getPushStopButton().setEnabled(true);

                        pushForm.getScheduleDetailLabel().setVisible(true);
                        pushForm.getScheduleDetailLabel().setText("计划任务执行中，下一次执行时间：" + latest5RunTimeList.get(0));

                        // 支持秒级别定时任务
                        CronUtil.setMatchSecond(true);
                        CronUtil.schedule(App.config.getTextCron(), (Task) () -> new PushRunThread().start());
                        CronUtil.start();
                    }
                    existScheduleTask = true;
                }

                if (!existScheduleTask) {
                    JOptionPane.showMessageDialog(pushPanel, "请先设置计划任务！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        })));

        // 线程数滑块
        pushForm.getThreadCountSlider().addChangeListener(e -> {
            int slideValue = pushForm.getThreadCountSlider().getValue();
            pushForm.getThreadCountTextField().setText(String.valueOf(slideValue));
            App.config.setThreadCount(slideValue);
            refreshPushInfo();
        });

        pushForm.getThreadTipsLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                CommonTipsDialog dialog = new CommonTipsDialog();

                StringBuilder tipsBuilder = new StringBuilder();
                tipsBuilder.append("<h1>线程数调整为多少比较合适？</h1>");
                tipsBuilder.append("<h2>建议不要超过100</h2>");
                tipsBuilder.append("<p>WePush的连接池管理尚未开发完毕</p>");
                tipsBuilder.append("<p>目前使用的是各自消息类型官方SDK(微信相关消息除外)内置的连接池</p>");
                tipsBuilder.append("<p>如果优先考虑推送的成功率而非速度建议30-50左右</p>");

                dialog.setHtmlText(tipsBuilder.toString());
                dialog.pack();
                dialog.setVisible(true);

                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                label.setIcon(new ImageIcon(UiConsts.HELP_FOCUSED_ICON));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setIcon(new ImageIcon(UiConsts.HELP_ICON));
                super.mouseExited(e);
            }
        });

        pushForm.getDryRunHelpLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                CommonTipsDialog dialog = new CommonTipsDialog();

                StringBuilder tipsBuilder = new StringBuilder();
                tipsBuilder.append("<h1>什么是空跑？</h1>");
                tipsBuilder.append("<h2>除了不会真实发送消息，其他与正常推送流程相同</h2>");
                tipsBuilder.append("<p>空跑模式可以验证消息数据以及流程的准确性</p>");
                tipsBuilder.append("<p>建议在执行真正推送之前先进行一遍空跑</p>");

                dialog.setHtmlText(tipsBuilder.toString());
                dialog.pack();
                dialog.setVisible(true);

                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                label.setIcon(new ImageIcon(UiConsts.HELP_FOCUSED_ICON));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setIcon(new ImageIcon(UiConsts.HELP_ICON));
                super.mouseExited(e);
            }
        });
    }

    static void refreshPushInfo() {
        PushForm pushForm = PushForm.getInstance();

        // 总记录数
        long totalCount = PushData.allUser.size();
        pushForm.getPushTotalCountLabel().setText("消息总数：" + totalCount);
        // 平均每个线程分配
        int threadCount = Integer.parseInt(pushForm.getThreadCountTextField().getText());
        int perThread = (int) (totalCount / threadCount) + 1;
        pushForm.getCountPerThread().setText("平均每个线程分配：" + perThread);
        // 可用处理器核心
        pushForm.getAvailableProcessorLabel().setText("可用处理器核心：" + Runtime.getRuntime().availableProcessors());
        // JVM内存占用
        pushForm.getJvmMemoryLabel().setText("JVM内存占用：" + FileUtil.readableFileSize(Runtime.getRuntime().totalMemory()) + "/" + FileUtil.readableFileSize(Runtime.getRuntime().maxMemory()));
    }

}
