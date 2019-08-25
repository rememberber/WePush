package com.fangxuele.tool.push.ui.frame;

import com.apple.eawt.Application;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.form.HttpResultForm;
import com.fangxuele.tool.push.util.ComponentUtil;
import com.fangxuele.tool.push.util.SystemUtil;
import com.google.common.collect.Lists;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * <pre>
 * Http请求响应结果展示frame
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/7/19.
 */
public class HttpResultFrame extends JFrame {

    private static final long serialVersionUID = 5950950940687769444L;

    private static HttpResultFrame httpResultFrame;

    public void init() {
        String title = "Http请求结果";
        this.setName(title);
        this.setTitle(title);
        List<Image> images = Lists.newArrayList();
        images.add(UiConsts.IMAGE_LOGO_1024);
        images.add(UiConsts.IMAGE_LOGO_512);
        images.add(UiConsts.IMAGE_LOGO_256);
        images.add(UiConsts.IMAGE_LOGO_128);
        images.add(UiConsts.IMAGE_LOGO_64);
        images.add(UiConsts.IMAGE_LOGO_48);
        images.add(UiConsts.IMAGE_LOGO_32);
        images.add(UiConsts.IMAGE_LOGO_24);
        images.add(UiConsts.IMAGE_LOGO_16);
        this.setIconImages(images);
        // Mac系统Dock图标
        if (SystemUtil.isMacOs()) {
            Application application = Application.getApplication();
            application.setDockIconImage(UiConsts.IMAGE_LOGO_1024);
        }

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.6, 0.66);
    }

    public static HttpResultFrame getInstance() {
        if (httpResultFrame == null) {
            httpResultFrame = new HttpResultFrame();
            httpResultFrame.init();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            if (screenSize.getWidth() <= 1366) {
                // 低分辨率下自动最大化窗口
                httpResultFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
            httpResultFrame.setContentPane(HttpResultForm.getInstance().getHttpResultPanel());
            httpResultFrame.pack();
        }

        return httpResultFrame;
    }

    public static void showResultWindow() {
        getInstance().setVisible(true);
    }
}
