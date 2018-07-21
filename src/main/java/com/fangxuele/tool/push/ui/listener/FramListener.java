package com.fangxuele.tool.push.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.MainWindow;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * 窗体事件监听
 * Created by rememberber(https://github.com/rememberber) on 2017/6/21.
 */
public class FramListener {

    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        MainWindow.frame.addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void windowIconified(WindowEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (!MainWindow.mainWindow.getPushStartButton().isEnabled()) {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getPushPanel(),
                            "有推送任务正在进行！\n\n为避免数据丢失，请先停止!\n\n", "Sorry~", JOptionPane.WARNING_MESSAGE);
                } else {
                    MainWindow.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                }

            }

            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }
        });
    }
}
