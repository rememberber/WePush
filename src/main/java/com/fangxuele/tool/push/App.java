package com.fangxuele.tool.push;

import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.form.LoadingForm;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.frame.MainFrame;
import com.fangxuele.tool.push.util.ConfigUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.UpgradeUtil;
import com.formdev.flatlaf.util.SystemInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;

import javax.swing.*;
import java.awt.*;

/**
 * <pre>
 * Main Enter!
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/4/20.
 */
@Slf4j
public class App {
    public static MainFrame mainFrame;

    public static ConfigUtil config = ConfigUtil.getInstance();

    public static SqlSession sqlSession = MybatisUtil.getSqlSession();

    public static SystemTray tray;

    public static TrayIcon trayIcon;

    public static void main(String[] args) {
        if (SystemInfo.isMacOS) {
//            java -Xdock:name="WePush" -Xdock:icon=WePush.jpg ... (whatever else you normally specify here)
//            java -Xms64m -Xmx256m -Dapple.awt.application.name="WePush" -Dcom.apple.mrj.application.apple.menu.about.name="WePush" -cp "./lib/*" com.luoboduner.moo.tool.App
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", UiConsts.APP_NAME);
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", UiConsts.APP_NAME);
            System.setProperty("apple.awt.application.appearance", "system");
        }

        Init.initTheme();
        mainFrame = new MainFrame();
        mainFrame.init();
        JPanel loadingPanel = new LoadingForm().getLoadingPanel();
        mainFrame.add(loadingPanel);
        mainFrame.pack();
        mainFrame.setVisible(true);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (config.isDefaultMaxWindow() || screenSize.getWidth() <= 1366) {
            // 低分辨率下自动最大化窗口
            mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
        UpgradeUtil.smoothUpgrade();

        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        Init.initGlobalFont();
        mainFrame.setContentPane(MainWindow.getInstance().getMainPanel());
        MainWindow.getInstance().init();
        Init.initAllTab();
        Init.initOthers();
        mainFrame.addListeners();
        mainFrame.remove(loadingPanel);
        Init.fontSizeGuide();
        Init.initTray();
    }
}
