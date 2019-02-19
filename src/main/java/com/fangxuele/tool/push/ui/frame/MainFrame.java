package com.fangxuele.tool.push.ui.frame;

import com.apple.eawt.Application;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.util.SystemUtil;

import javax.swing.*;
import java.awt.*;

/**
 * <pre>
 * 主窗口
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/2/14.
 */
public class MainFrame extends JFrame {
    public void init() {
        this.setName(UiConsts.APP_NAME);
        this.setIconImage(UiConsts.IMAGE_ICON);
        // Mac系统Dock图标
        if (SystemUtil.isMacOs()) {
            Application application = Application.getApplication();
            application.setDockIconImage(UiConsts.IMAGE_ICON);
        }

        //得到屏幕的尺寸
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setBounds((int) (screenSize.width * 0.1), (int) (screenSize.height * 0.08), (int) (screenSize.width * 0.8),
                (int) (screenSize.height * 0.8));

        Dimension preferSize = new Dimension((int) (screenSize.width * 0.8),
                (int) (screenSize.height * 0.8));
        this.setPreferredSize(preferSize);

        MainWindow.mainWindow = new MainWindow();

        Init.initOthers();
        Init.initAllTab();
    }
}
