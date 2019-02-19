package com.fangxuele.tool.push;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.frame.MainFrame;
import com.fangxuele.tool.push.ui.listener.AboutListener;
import com.fangxuele.tool.push.ui.listener.FramListener;
import com.fangxuele.tool.push.ui.listener.HelpListener;
import com.fangxuele.tool.push.ui.listener.MemberListener;
import com.fangxuele.tool.push.ui.listener.MsgListener;
import com.fangxuele.tool.push.ui.listener.PushHisListener;
import com.fangxuele.tool.push.ui.listener.PushListener;
import com.fangxuele.tool.push.ui.listener.ScheduleListener;
import com.fangxuele.tool.push.ui.listener.SettingListener;
import com.fangxuele.tool.push.ui.listener.TabListener;

import javax.swing.*;

/**
 * Main Enter!
 */
public class App {
    private static final Log log = LogFactory.get();

    public static MainFrame mainFrame;

    public static void main(String[] args) {
        // 初始化主题
        Init.initTheme();
        // 统一设置字体
        Init.initGlobalFont();

        mainFrame = new MainFrame();
        mainFrame.init();
        mainFrame.setContentPane(MainWindow.mainWindow.mainPanel);
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.pack();
        mainFrame.setVisible(true);

        // 添加事件监听
        AboutListener.addListeners();
        HelpListener.addListeners();
        PushHisListener.addListeners();
        SettingListener.addListeners();
        MsgListener.addListeners();
        MemberListener.addListeners();
        PushListener.addListeners();
        ScheduleListener.addListeners();
        TabListener.addListeners();
        FramListener.addListeners();
    }
}
