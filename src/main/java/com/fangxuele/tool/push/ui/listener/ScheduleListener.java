package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.date.DateUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.MainWindow;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

/**
 * 计划任务tab相关事件监听
 * Created by rememberber(https://github.com/rememberber) on 2017/6/28.
 */
public class ScheduleListener {
    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        MainWindow.mainWindow.getScheduleSaveButton().addActionListener(e -> {
            try {
                String textStartAt = MainWindow.mainWindow.getStartAtThisTimeTextField().getText();
                boolean isStartAt = MainWindow.mainWindow.getRunAtThisTimeRadioButton().isSelected();
                if (StringUtils.isNotEmpty(textStartAt)) {
                    if (DateUtil.parse(textStartAt).getTime() <= System.currentTimeMillis() && isStartAt) {
                        JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(),
                                "保存失败！\n\n开始推送时间不能小于系统当前时间！", "失败",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    Init.configer.setRadioStartAt(isStartAt);
                    Init.configer.setTextStartAt(textStartAt);
                } else if (isStartAt) {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(),
                            "保存失败！\n\n开始推送时间不能为空！", "失败",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    Init.configer.setRadioStartAt(isStartAt);
                    Init.configer.setTextStartAt(textStartAt);
                }

                String textPerDay = MainWindow.mainWindow.getStartPerDayTextField().getText();
                boolean isPerDay = MainWindow.mainWindow.getRunPerDayRadioButton().isSelected();
                if (StringUtils.isNotEmpty(textPerDay)) {
                    DateUtil.parse(textPerDay);
                    Init.configer.setRadioPerDay(isPerDay);
                    Init.configer.setTextPerDay(textPerDay);
                } else if (isPerDay) {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(),
                            "保存失败！\n\n每天固定推送时间不能为空！", "失败",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    Init.configer.setRadioPerDay(isPerDay);
                    Init.configer.setTextPerDay(textPerDay);
                }

                String textPerWeekTime = MainWindow.mainWindow.getStartPerWeekTextField().getText();
                boolean isPerWeek = MainWindow.mainWindow.getRunPerWeekRadioButton().isSelected();
                if (StringUtils.isNotEmpty(textPerWeekTime)) {
                    DateUtil.parse(textPerWeekTime);
                    Init.configer.setRadioPerWeek(isPerWeek);
                    Init.configer.setTextPerWeekWeek(MainWindow.mainWindow.getSchedulePerWeekComboBox().getSelectedItem().toString());
                    Init.configer.setTextPerWeekTime(textPerWeekTime);
                } else if (isPerWeek) {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(),
                            "保存失败！\n\n每周固定推送时间不能为空！", "失败",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    Init.configer.setRadioPerWeek(isPerWeek);
                    Init.configer.setTextPerWeekWeek(MainWindow.mainWindow.getSchedulePerWeekComboBox().getSelectedItem().toString());
                    Init.configer.setTextPerWeekTime(textPerWeekTime);
                }

                Init.configer.save();
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(), "保存成功！\n\n将在下一次按计划执行时生效！\n\n", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(), "保存失败！\n\n时间格式错误：" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        MainWindow.mainWindow.getRunAtThisTimeRadioButton().addActionListener(e -> {
            if (MainWindow.mainWindow.getRunAtThisTimeRadioButton().isSelected()) {
                MainWindow.mainWindow.getRunPerDayRadioButton().setSelected(false);
                MainWindow.mainWindow.getRunPerWeekRadioButton().setSelected(false);
            }
        });

        MainWindow.mainWindow.getRunPerDayRadioButton().addActionListener(e -> {
            if (MainWindow.mainWindow.getRunPerDayRadioButton().isSelected()) {
                MainWindow.mainWindow.getRunAtThisTimeRadioButton().setSelected(false);
                MainWindow.mainWindow.getRunPerWeekRadioButton().setSelected(false);
            }
        });

        MainWindow.mainWindow.getRunPerWeekRadioButton().addActionListener(e -> {
            if (MainWindow.mainWindow.getRunPerWeekRadioButton().isSelected()) {
                MainWindow.mainWindow.getRunAtThisTimeRadioButton().setSelected(false);
                MainWindow.mainWindow.getRunPerDayRadioButton().setSelected(false);
            }
        });
    }
}
