package com.fangxuele.tool.push.ui.frame;

import com.fangxuele.tool.push.ui.form.HttpResultForm;
import com.fangxuele.tool.push.util.ComponentUtil;

import javax.swing.*;
import java.awt.*;

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
