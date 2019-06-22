package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.date.DateUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.ScheduleForm;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
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
        ScheduleForm.scheduleForm.getScheduleSaveButton().addActionListener(e -> {
            try {
                String textStartAt = ScheduleForm.scheduleForm.getStartAtThisTimeTextField().getText();
                boolean isStartAt = ScheduleForm.scheduleForm.getRunAtThisTimeRadioButton().isSelected();
                if (StringUtils.isNotEmpty(textStartAt)) {
                    if (DateUtil.parse(textStartAt).getTime() <= System.currentTimeMillis() && isStartAt) {
                        JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(),
                                "保存失败！\n\n开始推送时间不能小于系统当前时间！", "失败",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    App.config.setRadioStartAt(isStartAt);
                    App.config.setTextStartAt(textStartAt);
                } else if (isStartAt) {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(),
                            "保存失败！\n\n开始推送时间不能为空！", "失败",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    App.config.setRadioStartAt(isStartAt);
                    App.config.setTextStartAt(textStartAt);
                }

                String textPerDay = ScheduleForm.scheduleForm.getStartPerDayTextField().getText();
                boolean isPerDay = ScheduleForm.scheduleForm.getRunPerDayRadioButton().isSelected();
                if (StringUtils.isNotEmpty(textPerDay)) {
                    DateUtil.parse(textPerDay);
                    App.config.setRadioPerDay(isPerDay);
                    App.config.setTextPerDay(textPerDay);
                } else if (isPerDay) {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(),
                            "保存失败！\n\n每天固定推送时间不能为空！", "失败",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    App.config.setRadioPerDay(isPerDay);
                    App.config.setTextPerDay(textPerDay);
                }

                String textPerWeekTime = ScheduleForm.scheduleForm.getStartPerWeekTextField().getText();
                boolean isPerWeek = ScheduleForm.scheduleForm.getRunPerWeekRadioButton().isSelected();
                if (StringUtils.isNotEmpty(textPerWeekTime)) {
                    DateUtil.parse(textPerWeekTime);
                    App.config.setRadioPerWeek(isPerWeek);
                    App.config.setTextPerWeekWeek(Objects.requireNonNull(ScheduleForm.scheduleForm.getSchedulePerWeekComboBox().getSelectedItem()).toString());
                    App.config.setTextPerWeekTime(textPerWeekTime);
                } else if (isPerWeek) {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(),
                            "保存失败！\n\n每周固定推送时间不能为空！", "失败",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    App.config.setRadioPerWeek(isPerWeek);
                    App.config.setTextPerWeekWeek(Objects.requireNonNull(ScheduleForm.scheduleForm.getSchedulePerWeekComboBox().getSelectedItem()).toString());
                    App.config.setTextPerWeekTime(textPerWeekTime);
                }

                App.config.setNeedReimport(ScheduleForm.scheduleForm.getReimportCheckBox().isSelected());
                App.config.setReimportWay((String) ScheduleForm.scheduleForm.getReimportComboBox().getSelectedItem());

                App.config.save();
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(), "保存成功！\n\n将在下一次按计划执行时生效！\n\n", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(), "保存失败！\n\n时间格式错误：" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        ScheduleForm.scheduleForm.getRunAtThisTimeRadioButton().addActionListener(e -> {
            if (ScheduleForm.scheduleForm.getRunAtThisTimeRadioButton().isSelected()) {
                ScheduleForm.scheduleForm.getRunPerDayRadioButton().setSelected(false);
                ScheduleForm.scheduleForm.getRunPerWeekRadioButton().setSelected(false);
            }
        });

        ScheduleForm.scheduleForm.getRunPerDayRadioButton().addActionListener(e -> {
            if (ScheduleForm.scheduleForm.getRunPerDayRadioButton().isSelected()) {
                ScheduleForm.scheduleForm.getRunAtThisTimeRadioButton().setSelected(false);
                ScheduleForm.scheduleForm.getRunPerWeekRadioButton().setSelected(false);
            }
        });

        ScheduleForm.scheduleForm.getRunPerWeekRadioButton().addActionListener(e -> {
            if (ScheduleForm.scheduleForm.getRunPerWeekRadioButton().isSelected()) {
                ScheduleForm.scheduleForm.getRunAtThisTimeRadioButton().setSelected(false);
                ScheduleForm.scheduleForm.getRunPerDayRadioButton().setSelected(false);
            }
        });
    }
}
