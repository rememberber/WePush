package com.fangxuele.tool.push.ui.listener;

import com.fangxuele.tool.push.App;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import static com.fangxuele.tool.push.App.mainFrame;
import static com.fangxuele.tool.push.ui.form.MainWindow.mainWindow;
import static com.fangxuele.tool.push.ui.form.PushForm.pushForm;

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
                if (!pushForm.getPushStartButton().isEnabled()) {
                    JOptionPane.showMessageDialog(mainWindow.getPushPanel(),
                            "有推送任务正在进行！\n\n为避免数据丢失，请先停止!\n\n", "Sorry~",
                            JOptionPane.WARNING_MESSAGE);
                } else {
                    App.sqlSession.close();
                    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
