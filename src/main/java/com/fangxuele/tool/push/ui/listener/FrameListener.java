package com.fangxuele.tool.push.ui.listener;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.util.SystemUtil;
import com.fangxuele.tool.push.util.UiThreadUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.desktop.AppReopenedListener;
import java.awt.event.*;

import static com.fangxuele.tool.push.App.mainFrame;

/**
 * <pre>
 * 窗体事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/21.
 */
public class FrameListener {

    public static void addListeners() {
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!App.config.isCloseToTray()) {
                    Init.shutdown();
                    return;
                }
                // macOS 红灯应对窗口 hide。WINDOW_CLOSING 里 ICONIFIED 会让红绿灯卡住。
                if (SystemUtil.isMacOs()) {
                    SwingUtilities.invokeLater(() -> mainFrame.setVisible(false));
                } else {
                    mainFrame.setExtendedState(JFrame.ICONIFIED);
                }
            }
        });

        addMacReopenHandler();

        // 鼠标双击最大化/还原
        App.mainFrame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    if (App.mainFrame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                        App.mainFrame.setExtendedState(JFrame.NORMAL);
                    } else {
                        App.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    }
                }
            }
        });

        MainWindow.getInstance().getMainPanel().registerKeyboardAction(e -> mainFrame.setExtendedState(Frame.ICONIFIED), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private static void addMacReopenHandler() {
        if (!SystemUtil.isMacOs() || !Desktop.isDesktopSupported()) {
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.APP_EVENT_REOPENED)) {
            return;
        }
        desktop.addAppEventListener((AppReopenedListener) e -> UiThreadUtil.runOnUi(FrameListener::restoreMainFrame));
    }

    private static void restoreMainFrame() {
        mainFrame.setVisible(true);
        if (App.config.isDefaultMaxWindow()) {
            mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else if ((mainFrame.getExtendedState() & JFrame.ICONIFIED) != 0) {
            mainFrame.setExtendedState(JFrame.NORMAL);
        }
        mainFrame.toFront();
        mainFrame.requestFocus();
    }
}
