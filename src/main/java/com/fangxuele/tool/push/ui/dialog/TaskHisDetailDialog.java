package com.fangxuele.tool.push.ui.dialog;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.util.ComponentUtil;
import com.fangxuele.tool.push.util.SystemUtil;
import com.formdev.flatlaf.util.SystemInfo;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;

public class TaskHisDetailDialog extends JDialog {
    private JPanel contentPane;
    private JTextArea textArea1;
    private JPanel pushUpPanel;
    private JLabel pushSuccessCount;
    private JLabel pushFailCount;
    private JLabel pushTotalProgressLabel;
    private JProgressBar pushTotalProgressBar;
    private JLabel pushLastTimeLabel;
    private JLabel jvmMemoryLabel;
    private JLabel availableProcessorLabel;
    private JLabel pushTotalCountLabel;
    private JLabel pushMsgName;
    private JLabel scheduleDetailLabel;
    private JLabel countPerThread;
    private JLabel pushLeftTimeLabel;
    private JLabel tpsLabel;
    private JPanel pushControlPanel;
    private JTextField threadCountTextField;
    private JButton pushStopButton;
    private JSlider threadCountSlider;

    public TaskHisDetailDialog() {
        super(App.mainFrame, "执行详情");
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.5, 0.64);
        setContentPane(contentPane);
        setModal(true);

        if (SystemUtil.isMacOs() && SystemInfo.isMacFullWindowContentSupported) {
            this.getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            this.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            this.getRootPane().putClientProperty("apple.awt.fullscreenable", true);
            this.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
            GridLayoutManager gridLayoutManager = (GridLayoutManager) contentPane.getLayout();
            gridLayoutManager.setMargin(new Insets(28, 0, 0, 0));
        }
        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // 设置滚动条速度
//        ScrollUtil.smoothPane(settingScrollPane);

    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 10, 0, 10), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        pushUpPanel = new JPanel();
        pushUpPanel.setLayout(new GridLayoutManager(7, 10, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(pushUpPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        pushSuccessCount = new JLabel();
        Font pushSuccessCountFont = this.$$$getFont$$$(null, -1, 72, pushSuccessCount.getFont());
        if (pushSuccessCountFont != null) pushSuccessCount.setFont(pushSuccessCountFont);
        pushSuccessCount.setForeground(new Color(-13587376));
        pushSuccessCount.setText("0");
        pushUpPanel.add(pushSuccessCount, new GridConstraints(0, 0, 7, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pushFailCount = new JLabel();
        Font pushFailCountFont = this.$$$getFont$$$(null, -1, 72, pushFailCount.getFont());
        if (pushFailCountFont != null) pushFailCount.setFont(pushFailCountFont);
        pushFailCount.setForeground(new Color(-2200483));
        pushFailCount.setText("0");
        pushUpPanel.add(pushFailCount, new GridConstraints(0, 2, 7, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pushTotalProgressLabel = new JLabel();
        pushTotalProgressLabel.setText("总进度");
        pushUpPanel.add(pushTotalProgressLabel, new GridConstraints(6, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pushTotalProgressBar = new JProgressBar();
        pushTotalProgressBar.setStringPainted(true);
        pushUpPanel.add(pushTotalProgressBar, new GridConstraints(6, 9, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("成功");
        pushUpPanel.add(label1, new GridConstraints(2, 1, 3, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("失败");
        pushUpPanel.add(label2, new GridConstraints(2, 3, 3, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        separator1.setOrientation(1);
        pushUpPanel.add(separator1, new GridConstraints(0, 4, 7, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        pushLastTimeLabel = new JLabel();
        pushLastTimeLabel.setEnabled(true);
        Font pushLastTimeLabelFont = this.$$$getFont$$$("Microsoft YaHei UI Light", -1, 36, pushLastTimeLabel.getFont());
        if (pushLastTimeLabelFont != null) pushLastTimeLabel.setFont(pushLastTimeLabelFont);
        pushLastTimeLabel.setForeground(new Color(-6710887));
        pushLastTimeLabel.setText("0s");
        pushUpPanel.add(pushLastTimeLabel, new GridConstraints(0, 6, 3, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setHorizontalAlignment(0);
        label3.setHorizontalTextPosition(0);
        label3.setText("耗时");
        pushUpPanel.add(label3, new GridConstraints(0, 5, 3, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        separator2.setOrientation(1);
        pushUpPanel.add(separator2, new GridConstraints(0, 7, 7, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        jvmMemoryLabel = new JLabel();
        jvmMemoryLabel.setText("JVM内存占用：--");
        pushUpPanel.add(jvmMemoryLabel, new GridConstraints(4, 8, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        availableProcessorLabel = new JLabel();
        availableProcessorLabel.setText("可用处理器核心：--");
        pushUpPanel.add(availableProcessorLabel, new GridConstraints(3, 8, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pushTotalCountLabel = new JLabel();
        pushTotalCountLabel.setText("消息总数：0");
        pushUpPanel.add(pushTotalCountLabel, new GridConstraints(1, 8, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pushMsgName = new JLabel();
        Font pushMsgNameFont = this.$$$getFont$$$(null, -1, 24, pushMsgName.getFont());
        if (pushMsgNameFont != null) pushMsgName.setFont(pushMsgNameFont);
        pushMsgName.setForeground(new Color(-276358));
        pushMsgName.setText("消息标题");
        pushUpPanel.add(pushMsgName, new GridConstraints(0, 8, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scheduleDetailLabel = new JLabel();
        scheduleDetailLabel.setForeground(new Color(-276358));
        scheduleDetailLabel.setText("");
        pushUpPanel.add(scheduleDetailLabel, new GridConstraints(5, 8, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        countPerThread = new JLabel();
        countPerThread.setText("平均每个线程分配：0");
        pushUpPanel.add(countPerThread, new GridConstraints(2, 8, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("预计剩余");
        pushUpPanel.add(label4, new GridConstraints(3, 5, 3, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pushLeftTimeLabel = new JLabel();
        Font pushLeftTimeLabelFont = this.$$$getFont$$$("Microsoft YaHei UI Light", -1, 36, pushLeftTimeLabel.getFont());
        if (pushLeftTimeLabelFont != null) pushLeftTimeLabel.setFont(pushLeftTimeLabelFont);
        pushLeftTimeLabel.setForeground(new Color(-6710887));
        pushLeftTimeLabel.setText("0s");
        pushUpPanel.add(pushLeftTimeLabel, new GridConstraints(3, 6, 3, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("TPS");
        pushUpPanel.add(label5, new GridConstraints(6, 5, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tpsLabel = new JLabel();
        tpsLabel.setText("0");
        pushUpPanel.add(tpsLabel, new GridConstraints(6, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 10, 0, 10), -1, -1));
        panel1.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel3.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textArea1 = new JTextArea();
        scrollPane1.setViewportView(textArea1);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 10, 10, 10), -1, -1));
        panel1.add(panel4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        pushControlPanel = new JPanel();
        pushControlPanel.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(pushControlPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("线程数");
        label6.setToolTipText("当前版本受http连接池限制建议不要设置过多线程，推荐100以内");
        pushControlPanel.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threadCountTextField = new JTextField();
        threadCountTextField.setEditable(false);
        threadCountTextField.setFocusable(false);
        threadCountTextField.setRequestFocusEnabled(false);
        threadCountTextField.setToolTipText("当前版本受http连接池限制建议不要设置过多线程，推荐100以内");
        pushControlPanel.add(threadCountTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(60, -1), null, 0, false));
        pushStopButton = new JButton();
        pushStopButton.setEnabled(false);
        pushStopButton.setIcon(new ImageIcon(getClass().getResource("/icon/suspend.png")));
        pushStopButton.setText("停止");
        pushControlPanel.add(pushStopButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threadCountSlider = new JSlider();
        threadCountSlider.setDoubleBuffered(true);
        threadCountSlider.setExtent(0);
        threadCountSlider.setFocusCycleRoot(false);
        threadCountSlider.setFocusTraversalPolicyProvider(false);
        threadCountSlider.setFocusable(false);
        threadCountSlider.setInverted(false);
        threadCountSlider.setMajorTickSpacing(10);
        threadCountSlider.setMinimum(1);
        threadCountSlider.setMinorTickSpacing(5);
        threadCountSlider.setOpaque(false);
        threadCountSlider.setOrientation(0);
        threadCountSlider.setPaintLabels(false);
        threadCountSlider.setPaintTicks(true);
        threadCountSlider.setPaintTrack(true);
        threadCountSlider.setRequestFocusEnabled(false);
        threadCountSlider.setSnapToTicks(false);
        threadCountSlider.setValueIsAdjusting(false);
        pushControlPanel.add(threadCountSlider, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}