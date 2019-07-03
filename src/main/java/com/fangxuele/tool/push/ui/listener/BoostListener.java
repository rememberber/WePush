package com.fangxuele.tool.push.ui.listener;

import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.dialog.CommonTipsDialog;
import com.fangxuele.tool.push.util.ComponentUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.fangxuele.tool.push.ui.form.BoostForm.boostForm;

/**
 * <pre>
 * 性能模式监听器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/7/3.
 */
public class BoostListener {

    public static void addListeners() {
        boostForm.getBoostModeHelpLabel().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                CommonTipsDialog dialog = new CommonTipsDialog();
                ComponentUtil.setPrefersizeAndLocateToCenter(dialog, 0.6, 0.64);
                StringBuilder tipsBuilder = new StringBuilder();
                tipsBuilder.append("<h1>什么是性能模式？</h1>");
                tipsBuilder.append("<h2>最大限度利用系统资源，提升性能，实验性地不断优化，以期获得更快速的批量推送效果</h2>");
                tipsBuilder.append("<p>利用异步HTTP、NIO等技术提高批量推送效率</p>");
                tipsBuilder.append("<p>不断学习使用新技术，优化无止境，不择手段地提升批量推送速度</p>");
                tipsBuilder.append("<p><strong>注意：性能模式下CPU、内存、网络连接资源占用过大，" +
                        "执行期间如果出现机器卡顿、浏览器无法访问等属正常现象，推送结束即可自动恢复。</strong></p>");

                dialog.setHtmlText(tipsBuilder.toString());
                dialog.pack();
                dialog.setVisible(true);

                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                label.setIcon(new ImageIcon(UiConsts.HELP_FOCUSED_ICON));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setIcon(new ImageIcon(UiConsts.HELP_ICON));
                super.mouseExited(e);
            }
        });
    }
}
