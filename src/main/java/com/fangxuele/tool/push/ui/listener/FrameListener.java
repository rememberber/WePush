package com.fangxuele.tool.push.ui.listener;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.ui.form.MainWindow;

import javax.swing.*;
import java.awt.*;
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
        mainFrame.addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                mainFrame.setExtendedState(JFrame.ICONIFIED);
                if (!App.config.isCloseToTray()) {
                    App.sqlSession.close();
                    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {
                if (App.config.isDefaultMaxWindow()) {
                    // 低分辨率下自动最大化窗口
                    App.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
            }
        });

        // 鼠标双击最大化/还原
        App.mainFrame.addMouseListener(new MouseListener() {
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

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        MainWindow.getInstance().getMainPanel().registerKeyboardAction(e -> mainFrame.setExtendedState(Frame.ICONIFIED), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
}
