package com.fangxuele.tool.wechat.push.ui.listener;

import com.fangxuele.tool.wechat.push.ui.Init;
import com.fangxuele.tool.wechat.push.ui.MainWindow;
import com.xiaoleilu.hutool.date.DateUtil;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.LogFactory;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 计划任务tab相关事件监听
 * Created by zhouy on 2017/6/28.
 */
public class ScheduleListener {
    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        MainWindow.mainWindow.getScheduleSaveButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String textStartAt = MainWindow.mainWindow.getStartAtThisTimeTextField().getText();
                    if (StringUtils.isNotEmpty(textStartAt)) {
                        boolean isStartAt = MainWindow.mainWindow.getRunAtThisTimeRadioButton().isSelected();
                        Init.configer.setRadioStartAt(isStartAt);
                        if (DateUtil.parse(textStartAt).getTime() <= System.currentTimeMillis() && isStartAt) {
                            JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(),
                                    "保存失败！\n\n开始推送时间不能小于系统当前时间！", "失败",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        Init.configer.setTextStartAt(textStartAt);
                    }

                    String textStopAt = MainWindow.mainWindow.getStopAtThisTimeTextField().getText();
                    if (StringUtils.isNotEmpty(textStopAt)) {
                        boolean isStopAt = MainWindow.mainWindow.getStopAtThisTimeRadioButton().isSelected();
                        Init.configer.setRadioStopAt(isStopAt);
                        if (DateUtil.parse(textStopAt).getTime() <= System.currentTimeMillis() && isStopAt) {
                            JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(),
                                    "保存失败！\n\n停止推送时间不能小于系统当前时间！", "失败",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        Init.configer.setTextStopAt(textStopAt);
                    }

                    String textPerDay = MainWindow.mainWindow.getStartPerDayTextField().getText();
                    if (StringUtils.isNotEmpty(textPerDay)) {
                        Init.configer.setRadioPerDay(MainWindow.mainWindow.getRunPerDayRadioButton().isSelected());
                        DateUtil.parse(textPerDay);
                        Init.configer.setTextPerDay(textPerDay);
                    }

                    String textPerWeekTime = MainWindow.mainWindow.getStartPerWeekTextField().getText();
                    if (StringUtils.isNotEmpty(textPerWeekTime)) {
                        Init.configer.setRadioPerWeek(MainWindow.mainWindow.getRunPerWeekRadioButton().isSelected());
                        Init.configer.setTextPerWeekWeek(MainWindow.mainWindow.getSchedulePerWeekComboBox().getSelectedItem().toString());
                        DateUtil.parse(textPerWeekTime);
                        Init.configer.setTextPerWeekTime(textPerWeekTime);
                    }

                    Init.configer.save();
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(), "保存成功！\n\n将在下一次按计划执行时生效！\n\n", "成功",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e1) {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSchedulePanel(), "保存失败！\n\n时间格式错误：" + e1.getMessage(), "失败",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
}
