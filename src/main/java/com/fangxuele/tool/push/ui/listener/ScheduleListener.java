package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.date.DateUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.hutool.cron.pattern.CronPatternUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.dialog.CommonTipsDialog;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.ScheduleForm;
import com.fangxuele.tool.push.ui.form.SettingForm;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.quartz.CronExpression;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * <pre>
 * 计划任务tab相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/28.
 */
public class ScheduleListener {
    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        ScheduleForm scheduleForm = ScheduleForm.getInstance();
        MainWindow mainWindow = MainWindow.getInstance();

        scheduleForm.getScheduleSaveButton().addActionListener(e -> {
            try {
                String textStartAt = scheduleForm.getStartAtThisTimeTextField().getText();
                boolean isStartAt = scheduleForm.getRunAtThisTimeRadioButton().isSelected();
                if (StringUtils.isNotEmpty(textStartAt)) {
                    if (DateUtil.parse(textStartAt).getTime() <= System.currentTimeMillis() && isStartAt) {
                        JOptionPane.showMessageDialog(mainWindow.getSchedulePanel(),
                                "保存失败！\n\n开始推送时间不能小于系统当前时间！", "失败",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    App.config.setRadioStartAt(isStartAt);
                    App.config.setTextStartAt(textStartAt);
                } else if (isStartAt) {
                    JOptionPane.showMessageDialog(mainWindow.getSchedulePanel(),
                            "保存失败！\n\n开始推送时间不能为空！", "失败",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    App.config.setRadioStartAt(isStartAt);
                    App.config.setTextStartAt(textStartAt);
                }

                String textPerDay = scheduleForm.getStartPerDayTextField().getText();
                boolean isPerDay = scheduleForm.getRunPerDayRadioButton().isSelected();
                if (StringUtils.isNotEmpty(textPerDay)) {
                    DateUtil.parse(textPerDay);
                    App.config.setRadioPerDay(isPerDay);
                    App.config.setTextPerDay(textPerDay);
                } else if (isPerDay) {
                    JOptionPane.showMessageDialog(mainWindow.getSchedulePanel(),
                            "保存失败！\n\n每天固定推送时间不能为空！", "失败",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    App.config.setRadioPerDay(isPerDay);
                    App.config.setTextPerDay(textPerDay);
                }

                String textPerWeekTime = scheduleForm.getStartPerWeekTextField().getText();
                boolean isPerWeek = scheduleForm.getRunPerWeekRadioButton().isSelected();
                if (StringUtils.isNotEmpty(textPerWeekTime)) {
                    DateUtil.parse(textPerWeekTime);
                    App.config.setRadioPerWeek(isPerWeek);
                    App.config.setTextPerWeekWeek(Objects.requireNonNull(scheduleForm.getSchedulePerWeekComboBox().getSelectedItem()).toString());
                    App.config.setTextPerWeekTime(textPerWeekTime);
                } else if (isPerWeek) {
                    JOptionPane.showMessageDialog(mainWindow.getSchedulePanel(),
                            "保存失败！\n\n每周固定推送时间不能为空！", "失败",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    App.config.setRadioPerWeek(isPerWeek);
                    App.config.setTextPerWeekWeek(Objects.requireNonNull(scheduleForm.getSchedulePerWeekComboBox().getSelectedItem()).toString());
                    App.config.setTextPerWeekTime(textPerWeekTime);
                }

                String textCron = scheduleForm.getCronTextField().getText();
                boolean isCron = scheduleForm.getCronRadioButton().isSelected();
                if (StringUtils.isNotEmpty(textCron)) {
                    try {
                        CronExpression.validateExpression(textCron);
                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(mainWindow.getSchedulePanel(),
                                "保存失败！\n\n无效的Cron表达式！\n" + e1.toString(), "失败",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (isCron) {
                        List<String> latest5RunTimeList = Lists.newArrayList();
                        Date now = new Date();
                        for (int i = 0; i < 5; i++) {
                            Date date = CronPatternUtil.nextDateAfter(new CronPattern(textCron), DateUtils.addDays(now, i), true);
                            latest5RunTimeList.add(DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss"));
                        }
                        JOptionPane.showMessageDialog(mainWindow.getSchedulePanel(),
                                "最近5次运行时间:\n" + String.join("\n", latest5RunTimeList), "提示",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                    App.config.setRadioCron(isCron);
                    App.config.setTextCron(textCron);
                } else if (isCron) {
                    JOptionPane.showMessageDialog(mainWindow.getSchedulePanel(),
                            "保存失败！\n\nCron表达式不能为空！", "失败",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    App.config.setRadioCron(isCron);
                    App.config.setTextCron(textCron);
                }

                if (scheduleForm.getSendPushResultCheckBox().isSelected() &&
                        (StringUtils.isBlank(SettingForm.getInstance().getMailHostTextField().getText())
                                || StringUtils.isBlank(SettingForm.getInstance().getMailFromTextField().getText()))) {
                    JOptionPane.showMessageDialog(mainWindow.getSchedulePanel(),
                            "保存失败！\n\n请先在设置中设置E-Mail服务器！", "失败",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                App.config.setSendPushResult(scheduleForm.getSendPushResultCheckBox().isSelected());
                App.config.setMailResultTos(scheduleForm.getMailResultToTextField().getText());

                App.config.setNeedReimport(scheduleForm.getReimportCheckBox().isSelected());
                App.config.setReimportWay((String) scheduleForm.getReimportComboBox().getSelectedItem());

                App.config.save();
                JOptionPane.showMessageDialog(mainWindow.getSchedulePanel(), "保存成功！\n\n将在下一次按计划执行时生效！\n\n", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(mainWindow.getSchedulePanel(), "保存失败！\n\n时间格式错误：" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        scheduleForm.getRunAtThisTimeRadioButton().addActionListener(e -> {
            if (scheduleForm.getRunAtThisTimeRadioButton().isSelected()) {
                scheduleForm.getRunPerDayRadioButton().setSelected(false);
                scheduleForm.getRunPerWeekRadioButton().setSelected(false);
                scheduleForm.getCronRadioButton().setSelected(false);
            }
        });

        scheduleForm.getRunPerDayRadioButton().addActionListener(e -> {
            if (scheduleForm.getRunPerDayRadioButton().isSelected()) {
                scheduleForm.getRunAtThisTimeRadioButton().setSelected(false);
                scheduleForm.getRunPerWeekRadioButton().setSelected(false);
                scheduleForm.getCronRadioButton().setSelected(false);
            }
        });

        scheduleForm.getRunPerWeekRadioButton().addActionListener(e -> {
            if (scheduleForm.getRunPerWeekRadioButton().isSelected()) {
                scheduleForm.getRunAtThisTimeRadioButton().setSelected(false);
                scheduleForm.getRunPerDayRadioButton().setSelected(false);
                scheduleForm.getCronRadioButton().setSelected(false);
            }
        });

        scheduleForm.getCronRadioButton().addActionListener(e -> {
            if (scheduleForm.getCronRadioButton().isSelected()) {
                scheduleForm.getRunAtThisTimeRadioButton().setSelected(false);
                scheduleForm.getRunPerDayRadioButton().setSelected(false);
                scheduleForm.getRunPerWeekRadioButton().setSelected(false);
            }
        });

        scheduleForm.getCronHelpLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                CommonTipsDialog dialog = new CommonTipsDialog();

                StringBuilder tipsBuilder = new StringBuilder();
                tipsBuilder.append("<h1>什么是Cron表达式？</h1>");
                tipsBuilder.append("<a href='https://baike.baidu.com/item/cron#3'>百度百科</a>");
                tipsBuilder.append("<p>举几个例子:</p>\n");
                tipsBuilder.append("<p>\"0 0 2 1 * ? *\" 表示在每月的1日的凌晨2点调度任务</p>\n");
                tipsBuilder.append("<p>\"0 15 10 ? * MON-FRI\" 表示周一到周五每天上午10：15执行作业</p>\n");
                tipsBuilder.append("<p>\"0 15 10 ? * 6L 2002-2006\" 表示2002-2006年的每个月的最后一个星期五上午10:15执行作</p>\n");
                tipsBuilder.append("<p>\"0 0 10,14,16 * * ?\" 每天上午10点，下午2点，4点</p>\n");
                tipsBuilder.append("<p>\"0 0/30 9-17 * * ?\" 朝九晚五工作时间内每半小时</p>\n");
                tipsBuilder.append("<p>\"0 0 12 ? * WED\" 表示每个星期三中午12点</p>\n");
                tipsBuilder.append("<p>\"0 0 12 * * ?\" 每天中午12点触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 ? * *\" 每天上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 * * ?\" 每天上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 * * ? *\" 每天上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 * * ? 2005\" 2005年的每天上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 * 14 * * ?\" 在每天下午2点到下午2:59期间的每1分钟触发</p>\n");
                tipsBuilder.append("<p>\"0 0/5 14 * * ?\" 在每天下午2点到下午2:55期间的每5分钟触发</p>\n");
                tipsBuilder.append("<p>\"0 0/5 14,18 * * ?\" 在每天下午2点到2:55期间和下午6点到6:55期间的每5分钟触发</p>\n");
                tipsBuilder.append("<p>\"0 0-5 14 * * ?\" 在每天下午2点到下午2:05期间的每1分钟触发</p>\n");
                tipsBuilder.append("<p>\"0 10,44 14 ? 3 WED\" 每年三月的星期三的下午2:10和2:44触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 ? * MON-FRI\" 周一至周五的上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 15 * ?\" 每月15日上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 L * ?\" 每月最后一日的上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 ? * 6L\" 每月的最后一个星期五上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 ? * 6L 2002-2005\" 2002年至2005年的每月的最后一个星期五上午10:15触发</p>\n");
                tipsBuilder.append("<p>\"0 15 10 ? * 6#3\" 每月的第三个星期五上午10:15触发");

                dialog.setHtmlText(tipsBuilder.toString());
                dialog.getTextPane1().setCaretPosition(0);
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

        scheduleForm.getCronOnlineLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("http://cron.qqe2.com/"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

        });
    }
}
