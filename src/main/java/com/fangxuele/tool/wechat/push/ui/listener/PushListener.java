package com.fangxuele.tool.wechat.push.ui.listener;

import com.fangxuele.tool.wechat.push.logic.PushData;
import com.fangxuele.tool.wechat.push.logic.RunPushThread;
import com.fangxuele.tool.wechat.push.ui.Init;
import com.fangxuele.tool.wechat.push.ui.MainWindow;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.LogFactory;

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
                new RunPushThread().start();
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
                        if (Init.configer.isRadioStartAt()) {

                        }
                        if (Init.configer.isRadioStopAt()) {

                        }
                        if (Init.configer.isRadioPerDay()) {

                        }
                        if (Init.configer.isRadioPerWeek()) {

                        }
                    }
                }).start();
            }
        });
    }

}
