package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.fangxuele.tool.push.logic.BoostPushRunThread;
import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.dialog.CommonTipsDialog;
import com.fangxuele.tool.push.ui.form.BoostForm;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.util.ComponentUtil;
import org.apache.commons.lang3.StringUtils;

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
                ComponentUtil.setPrefersizeAndLocateToCenter(dialog, 0.6, 0.7);
                StringBuilder tipsBuilder = new StringBuilder();
                tipsBuilder.append("<h1>什么是性能模式？</h1>");
                tipsBuilder.append("<h2>最大限度利用系统资源，提升性能，实验性地不断优化，以期获得更快速的批量推送效果</h2>");
                tipsBuilder.append("<p>利用异步HTTP、NIO等技术提高批量推送效率</p>");
                tipsBuilder.append("<p>不断学习使用新技术，优化无止境，不择手段地提升批量推送速度</p>");
                tipsBuilder.append("<p>一个人的力量有限，也希望更多技术大佬提供帮助和支持，一起挑战HTTP极限！</p>");
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

        // 开始按钮事件
        BoostForm.boostForm.getStartButton().addActionListener((e) -> ThreadUtil.execute(() -> {
            if (checkBeforePush()) {
                int isPush = JOptionPane.showConfirmDialog(boostForm.getBoostPanel(),
                        "确定开始推送吗？\n\n推送消息：" +
                                MessageEditForm.messageEditForm.getMsgNameField().getText() +
                                "\n推送人数：" + PushData.allUser.size() +
                                "\n\n空跑模式：" +
                                BoostForm.boostForm.getDryRunCheckBox().isSelected() + "\n", "确认推送？",
                        JOptionPane.YES_NO_OPTION);
                if (isPush == JOptionPane.YES_OPTION) {
                    ThreadUtil.execute(new BoostPushRunThread());
                }
            }
        }));
    }

    /**
     * 推送前检查
     *
     * @return boolean
     */
    private static boolean checkBeforePush() {
        if (StringUtils.isEmpty(MessageEditForm.messageEditForm.getMsgNameField().getText())) {
            JOptionPane.showMessageDialog(boostForm.getBoostPanel(), "请先选择一条消息！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            MainWindow.mainWindow.getTabbedPane().setSelectedIndex(2);

            return false;
        }
        if (PushData.allUser == null || PushData.allUser.size() == 0) {
            JOptionPane.showMessageDialog(boostForm.getBoostPanel(), "请先准备目标用户！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);

            return false;
        }
        return true;
    }

    static void refreshPushInfo() {
        // 总记录数
        long totalCount = PushData.allUser.size();
        BoostForm.boostForm.getMemberCountLabel().setText("消息总数：" + totalCount);
        // 可用处理器核心
        BoostForm.boostForm.getProcessorCountLabel().setText("可用处理器核心：" + Runtime.getRuntime().availableProcessors());
        // JVM内存占用
        BoostForm.boostForm.getJvmMemoryLabel().setText("JVM内存占用：" + FileUtil.readableFileSize(Runtime.getRuntime().totalMemory()) + "/" + FileUtil.readableFileSize(Runtime.getRuntime().maxMemory()));
    }
}
