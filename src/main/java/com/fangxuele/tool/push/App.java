package com.fangxuele.tool.push;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.frame.MainFrame;

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
        mainFrame.addListeners();
        mainFrame.setContentPane(MainWindow.mainWindow.mainPanel);
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }
}
