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
                if (checkBeforePush()) {
                    new RunPushThread().start();
                }
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
                            if (Init.configer.isRadioStartAt()) {
                                if (DateUtil.parse(Init.configer.getTextStartAt(), "yyyy-MM-dd HH:mm:ss").getTime() < System.currentTimeMillis()) {
                                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getPushPanel(), "计划开始推送时间不能小于系统当前时间！", "提示",
                                            JOptionPane.INFORMATION_MESSAGE);
                                    return;
                                }

                            }
                            if (Init.configer.isRadioStopAt()) {

                            }
                            if (Init.configer.isRadioPerDay()) {

                            }
                            if (Init.configer.isRadioPerWeek()) {

                            }

                            JOptionPane.showMessageDialog(MainWindow.mainWindow.getPushPanel(), "请先设置计划任务！", "提示",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }).start();
            }
        });
    }

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
