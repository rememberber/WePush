package com.fangxuele.tool.push.ui;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.ui.dialog.FontSizeAdjustDialog;
import com.fangxuele.tool.push.ui.form.*;
import com.fangxuele.tool.push.util.SystemUtil;
import com.fangxuele.tool.push.util.UIUtil;
import com.fangxuele.tool.push.util.UpgradeUtil;
import com.formdev.flatlaf.*;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.intellijthemes.*;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubDarkIJTheme;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
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
                fontSize = 13;
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

    }

    /**
     * 初始化look and feel
     */
    public static void initTheme() {
        try {
            switch (App.config.getTheme()) {
                case "系统默认":
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
                case "Flat Light":
                    setAccentColor();
                    FlatLightLaf.setup();
                    break;
                case "Flat IntelliJ":
                    setAccentColor();
                    FlatIntelliJLaf.setup();
                    break;
                case "Flat Dark":
                    setAccentColor();
                    FlatDarkLaf.setup();
                    break;
                case "Dark purple":
                    FlatDarkPurpleIJTheme.setup();
                    break;
                case "IntelliJ Cyan":
                    FlatCyanLightIJTheme.setup();
                    break;
                case "IntelliJ Light":
                    FlatLightFlatIJTheme.setup();
                    break;
                case "Monocai":
                    FlatMonocaiIJTheme.setup();
                    break;
                case "Monokai Pro":
                    FlatMonokaiProIJTheme.setup();
                    UIManager.put("Button.arc", 5);
                    break;
                case "One Dark":
                    FlatOneDarkIJTheme.setup();
                    break;
                case "Gray":
                    FlatGrayIJTheme.setup();
                    break;
                case "High contrast":
                    FlatHighContrastIJTheme.setup();
                    break;
                case "GitHub Dark":
                    FlatGitHubDarkIJTheme.setup();
                    break;
                case "Xcode-Dark":
                    FlatXcodeDarkIJTheme.setup();
                    break;
                case "Vuesion":
                    FlatVuesionIJTheme.setup();
                    break;
                case "Flat macOS Light":
                    FlatMacLightLaf.setup();
                    break;
                case "Flat macOS Dark":
                    FlatMacDarkLaf.setup();
                    break;
                default:
                    setAccentColor();
                    FlatDarculaLaf.setup();
            }

            if (FlatLaf.isLafDark()) {
//                FlatSVGIcon.ColorFilter.getInstance().setMapper(color -> color.brighter().brighter());
            } else {
                FlatSVGIcon.ColorFilter.getInstance().setMapper(color -> color.darker().darker());
//                SwingUtilities.windowForComponent(App.mainFrame).repaint();
            }

            if (App.config.isUnifiedBackground()) {
                UIManager.put("TitlePane.unifiedBackground", true);
            }

        } catch (Exception e) {
            logger.error(e);
        }
    }

    private static void setAccentColor() {
//        String accentColor = App.config.getAccentColor();
//        FlatLaf.setGlobalExtraDefaults((!accentColor.equals(SettingDialog.accentColorKeys[0]))
//                ? Collections.singletonMap("@accentColor", "$" + accentColor)
//                : null);
    }

    /**
     * 初始化所有tab
     */
    public static void initAllTab() {
        MessageTypeForm.init();
        ThreadUtil.execute(HelpForm::init);
//        ThreadUtil.execute(UserCaseForm::init);
        ThreadUtil.execute(AccountManageForm::init);
        ThreadUtil.execute(() -> AccountEditForm.init(null));
        ThreadUtil.execute(MessageManageForm::init);
        ThreadUtil.execute(PeopleManageForm::init);
        ThreadUtil.execute(() -> PeopleEditForm.init(null));
        ThreadUtil.execute(TaskForm::init);

        // 检查新版版
        if (App.config.isAutoCheckUpdate()) {
            ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(1);
            threadPoolExecutor.scheduleAtFixedRate(() -> UpgradeUtil.checkUpdate(true), 0, 24, TimeUnit.HOURS);
        }
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
                MenuItem exitItem = new MenuItem("Exit");

                openItem.addActionListener(e -> {
                    App.mainFrame.setExtendedState(JFrame.NORMAL);
                    App.mainFrame.setVisible(true);
                    App.mainFrame.requestFocus();
                });
                exitItem.addActionListener(e -> {
                    shutdown();
                });

                popupMenu.add(openItem);
                popupMenu.add(exitItem);

                if (SystemUtil.isWindowsOs()) {
                    App.trayIcon = new TrayIcon(UiConsts.IMAGE_LOGO_64, "WePush", popupMenu);
                } else {
                    App.trayIcon = new TrayIcon(new FlatSVGIcon("icon/icon_push.svg").getImage(), "WePush", popupMenu);
                }
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

    public static void shutdown() {
        App.config.save();
        App.sqlSession.close();
        App.mainFrame.dispose();
        System.exit(0);
    }
}
