package com.fangxuele.tool.push.ui;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.ui.dialog.FontSizeAdjustDialog;
import com.fangxuele.tool.push.ui.form.*;
import com.fangxuele.tool.push.ui.listener.AboutListener;
import com.fangxuele.tool.push.util.SystemUtil;
import com.fangxuele.tool.push.util.UIUtil;
import com.fangxuele.tool.push.util.UpgradeUtil;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.IntelliJTheme;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <pre>
 * 初始化类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/15.
 */
public class Init {

    private static final Log logger = LogFactory.get();

    /**
     * 字号初始化KEY
     */
    public static final String FONT_SIZE_INIT_PROP = "fontSizeInit";

    /**
     * 设置全局字体
     */
    public static void initGlobalFont() {
        if (StringUtils.isEmpty(App.config.getProps(FONT_SIZE_INIT_PROP))) {
            // 根据DPI调整字号
            // 得到屏幕的分辨率dpi
            // dell 1920*1080/24寸=96
            // 小米air 1920*1080/13.3寸=144
            // 小米air 1366*768/13.3寸=96
            int fontSize = 12;

            // Mac等高分辨率屏幕字号初始化
            if (SystemUtil.isMacOs()) {
                fontSize = 15;
            } else {
                fontSize = (int) (UIUtil.getScreenScale() * fontSize);
            }
            App.config.setFontSize(fontSize);
            App.config.save();
        }

        Font font = new Font(App.config.getFont(), Font.PLAIN, App.config.getFontSize());
        FontUIResource fontRes = new FontUIResource(font);
        for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, fontRes);
            }
        }

    }

    /**
     * 其他初始化
     */
    public static void initOthers() {
        // 设置版本
        AboutForm.getInstance().getVersionLabel().setText(UiConsts.APP_VERSION);
    }

    /**
     * 初始化look and feel
     */
    public static void initTheme() {
        if (SystemUtil.isMacM1() || SystemUtil.isLinuxOs()) {
            try {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarculaLaf");
                logger.warn("FlatDarculaLaf theme set.");
            } catch (Exception e) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e2) {
                    logger.error(ExceptionUtils.getStackTrace(e2));
                }
                logger.error(ExceptionUtils.getStackTrace(e));
            }
            return;
        }

        UIManager.put("TitlePane.unifiedBackground", true);

        try {
            switch (App.config.getTheme()) {
                case "系统默认":
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
                case "Flat Light":
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    FlatLightLaf.install();
                    break;
                case "Flat IntelliJ":
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    UIManager.setLookAndFeel("com.formdev.flatlaf.FlatIntelliJLaf");
                    break;
                case "Flat Dark":
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
                    break;
                case "Flat Darcula(推荐)":
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarculaLaf");
//                    UIManager.put( "TitlePane.unifiedBackground", true );
/**
 If you don't like/want it, you can disable it with:
 UIManager.put( "TitlePane.useWindowDecorations", false );

 It is also possible to disable only the embedded menu bar (and keep the dark title pane) with:
 UIManager.put( "TitlePane.menuBarEmbedded", false );

 It is also possible to disable this on command line with following VM options:
 -Dflatlaf.useWindowDecorations=false
 -Dflatlaf.menuBarEmbedded=false

 If you have following code in your app, you can remove it (no longer necessary):
 // enable window decorations
 JFrame.setDefaultLookAndFeelDecorated( true );
 JDialog.setDefaultLookAndFeelDecorated( true );
 **/
                    break;
                case "Dark purple":
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    IntelliJTheme.install(App.class.getResourceAsStream(
                            "/theme/DarkPurple.theme.json"));
                    break;
                case "IntelliJ Cyan":
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    IntelliJTheme.install(App.class.getResourceAsStream(
                            "/theme/Cyan.theme.json"));
                    break;
                case "IntelliJ Light":
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    IntelliJTheme.install(App.class.getResourceAsStream(
                            "/theme/Light.theme.json"));
                    break;

                case "BeautyEye":
                case "weblaf":
                case "Darcula":
                case "Darcula(推荐)":
                default:
                    if (SystemUtil.isJBR()) {
                        JFrame.setDefaultLookAndFeelDecorated(true);
                        JDialog.setDefaultLookAndFeelDecorated(true);
                    }
                    UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarculaLaf");
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * 初始化所有tab
     */
    public static void initAllTab() {
        ThreadUtil.execute(AboutForm::init);
        MessageTypeForm.init();
        ThreadUtil.execute(HelpForm::init);
//        ThreadUtil.execute(UserCaseForm::init);
        ThreadUtil.execute(() -> MessageEditForm.init(null));
        ThreadUtil.execute(MessageManageForm::init);
        ThreadUtil.execute(MemberForm::init);
        ThreadUtil.execute(PushForm::init);
        ThreadUtil.execute(BoostForm::init);
        ThreadUtil.execute(InfinityForm::init);
        ThreadUtil.execute(ScheduleForm::init);
        ThreadUtil.execute(SettingForm::init);
        ThreadUtil.execute(PushHisForm::init);

        // 检查新版版
        if (App.config.isAutoCheckUpdate()) {
            ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(1);
            threadPoolExecutor.scheduleAtFixedRate(() -> UpgradeUtil.checkUpdate(true), 0, 24, TimeUnit.HOURS);
        }
        // 更新二维码
        ThreadUtil.execute(AboutListener::initQrCode);
    }

    /**
     * 引导用户调整字号
     */
    public static void fontSizeGuide() {
        if (StringUtils.isEmpty(App.config.getProps(FONT_SIZE_INIT_PROP))) {
            FontSizeAdjustDialog fontSizeAdjustDialog = new FontSizeAdjustDialog();
            fontSizeAdjustDialog.pack();
            fontSizeAdjustDialog.setVisible(true);
        }

        App.config.setProps(FONT_SIZE_INIT_PROP, "true");
        App.config.save();
    }

    /**
     * 初始化系统托盘
     */
    public static void initTray() {

        try {
            if (App.config.isUseTray() && SystemTray.isSupported()) {
                App.tray = SystemTray.getSystemTray();

                PopupMenu popupMenu = new PopupMenu();
                popupMenu.setFont(App.mainFrame.getContentPane().getFont());

                MenuItem openItem = new MenuItem("WePush");
                MenuItem exitItem = new MenuItem("退出");

                openItem.addActionListener(e -> {
                    App.mainFrame.setExtendedState(JFrame.NORMAL);
                    App.mainFrame.setVisible(true);
                    App.mainFrame.requestFocus();
                });
                exitItem.addActionListener(e -> {
                    if (!PushForm.getInstance().getPushStartButton().isEnabled()) {
                        JOptionPane.showMessageDialog(MainWindow.getInstance().getPushPanel(),
                                "有推送任务正在进行！\n\n为避免数据丢失，请先停止!\n\n", "Sorry~",
                                JOptionPane.WARNING_MESSAGE);
                    } else {
                        App.config.save();
                        App.sqlSession.close();
                        App.mainFrame.dispose();
                        System.exit(0);
                    }
                });

                popupMenu.add(openItem);
                popupMenu.add(exitItem);

                App.trayIcon = new TrayIcon(UiConsts.IMAGE_LOGO_64, "WePush", popupMenu);
                App.trayIcon.setImageAutoSize(true);

                App.trayIcon.addActionListener(e -> {
                    App.mainFrame.setExtendedState(JFrame.NORMAL);
                    App.mainFrame.setVisible(true);
                    App.mainFrame.requestFocus();
                });
                App.trayIcon.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        switch (e.getButton()) {
                            case MouseEvent.BUTTON1: {
                                App.mainFrame.setExtendedState(JFrame.NORMAL);
                                App.mainFrame.setVisible(true);
                                App.mainFrame.requestFocus();
                                break;
                            }
                            case MouseEvent.BUTTON2: {
                                logger.debug("托盘图标被鼠标中键被点击");
                                break;
                            }
                            case MouseEvent.BUTTON3: {
                                logger.debug("托盘图标被鼠标右键被点击");
                                break;
                            }
                            default: {
                                break;
                            }
                        }
                    }
                });

                try {
                    App.tray.add(App.trayIcon);
                } catch (AWTException e) {
                    e.printStackTrace();
                    logger.error(ExceptionUtils.getStackTrace(e));
                }

            }

        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }
}
