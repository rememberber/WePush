package com.fangxuele.tool.push.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.MainWindow;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by rememberber(https://github.com/rememberber) on 2018/2/9.
 */
public class HelpListener {
    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        MainWindow.mainWindow.getLabelOnlineHelp().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://gitee.com/zhoubochina/WePush/wikis/help"));
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                MainWindow.mainWindow.getLabelOnlineHelp().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

        });
    }
}
