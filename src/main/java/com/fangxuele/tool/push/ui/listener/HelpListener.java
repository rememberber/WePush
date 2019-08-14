package com.fangxuele.tool.push.ui.listener;

import com.fangxuele.tool.push.ui.form.HelpForm;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * <pre>
 * HelpPanel Listener
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2018/2/9.
 */
public class HelpListener {

    public static void addListeners() {
        HelpForm.getInstance().getLabelOnlineHelp().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://gitee.com/zhoubochina/WePush/wikis/help"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                HelpForm.getInstance().getLabelOnlineHelp().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

        });
    }
}
