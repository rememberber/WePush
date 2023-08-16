package com.fangxuele.tool.push.ui.dialog;

import cn.hutool.core.thread.ThreadUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.util.ComponentUtil;
import com.fangxuele.tool.push.util.SystemUtil;
import com.fangxuele.tool.push.util.UpgradeUtil;
import com.formdev.flatlaf.util.SystemInfo;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

@Slf4j
public class AboutDialog extends JDialog {
    private JPanel contentPane;
    private JPanel aboutPanel;
    private JLabel sloganLabel;
    private JLabel qrCodeLabel;
    private JLabel versionLabel;
    private JLabel checkUpdateLabel;
    private JLabel companyLabel;
    private JLabel helpDocLabel;
    private JLabel pushTotalLabel;

    public AboutDialog() {
        super(App.mainFrame, "关于");
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.4, 0.64);
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

        addListeners();

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        pushTotalLabel.setText("<html>已累计为您推送 <b>" + App.config.getPushTotal() + "</b> 条消息</html>");
        // 设置版本
        versionLabel.setText(UiConsts.APP_VERSION);

        ThreadUtil.execute(this::initQrCode);

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public void addListeners() {
        companyLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://gitee.com/zhoubochina/WePush"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

        });

        // 检查更新
        checkUpdateLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                ThreadUtil.execute(() -> UpgradeUtil.checkUpdate(false));
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });

        // 帮助文档
        helpDocLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://github.com/rememberber/WePush/wiki"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
    }

    /**
     * 初始化二维码
     */
    public void initQrCode() {
        try {
            URL url = new URL(UiConsts.QR_CODE_URL);
            BufferedImage image = ImageIO.read(url);
            qrCodeLabel.setIcon(new ImageIcon(image));
        } catch (IOException e) {
            e.printStackTrace();
            log.error(ExceptionUtils.getStackTrace(e));
        }

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
        final JScrollPane scrollPane1 = new JScrollPane();
        contentPane.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        aboutPanel = new JPanel();
        aboutPanel.setLayout(new GridLayoutManager(9, 2, new Insets(0, 0, 10, 0), -1, -1));
        scrollPane1.setViewportView(aboutPanel);
        sloganLabel = new JLabel();
        sloganLabel.setIcon(new ImageIcon(getClass().getResource("/icon/logo-128.png")));
        sloganLabel.setText("");
        aboutPanel.add(sloganLabel, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setEnabled(true);
        Font label1Font = this.$$$getFont$$$(null, -1, 36, label1.getFont());
        if (label1Font != null) label1.setFont(label1Font);
        label1.setText("WePush");
        aboutPanel.add(label1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        qrCodeLabel = new JLabel();
        qrCodeLabel.setIcon(new ImageIcon(getClass().getResource("/icon/wx-zanshang.jpg")));
        qrCodeLabel.setText("");
        qrCodeLabel.setToolTipText("感谢您的鼓励和支持！");
        aboutPanel.add(qrCodeLabel, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        versionLabel = new JLabel();
        Font versionLabelFont = this.$$$getFont$$$("Microsoft YaHei UI", -1, -1, versionLabel.getFont());
        if (versionLabelFont != null) versionLabel.setFont(versionLabelFont);
        versionLabel.setText("v_0.0.0");
        aboutPanel.add(versionLabel, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkUpdateLabel = new JLabel();
        Font checkUpdateLabelFont = this.$$$getFont$$$(null, Font.BOLD, -1, checkUpdateLabel.getFont());
        if (checkUpdateLabelFont != null) checkUpdateLabel.setFont(checkUpdateLabelFont);
        checkUpdateLabel.setText("检查更新");
        aboutPanel.add(checkUpdateLabel, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        companyLabel = new JLabel();
        Font companyLabelFont = this.$$$getFont$$$("Microsoft YaHei UI", -1, -1, companyLabel.getFont());
        if (companyLabelFont != null) companyLabel.setFont(companyLabelFont);
        companyLabel.setText("<html>Fork me on Gitee: <a href='https://github.com/rememberber/wepush'>https://gitee.com/zhoubochina/WePush</a></html>");
        aboutPanel.add(companyLabel, new GridConstraints(8, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        helpDocLabel = new JLabel();
        Font helpDocLabelFont = this.$$$getFont$$$(null, Font.BOLD, -1, helpDocLabel.getFont());
        if (helpDocLabelFont != null) helpDocLabel.setFont(helpDocLabelFont);
        helpDocLabel.setText("| 帮助文档");
        aboutPanel.add(helpDocLabel, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pushTotalLabel = new JLabel();
        pushTotalLabel.setText("<html>已累计为您推送 <b>0</b> 条消息</html>");
        pushTotalLabel.setToolTipText(" 自3.4.0版本开始算起");
        aboutPanel.add(pushTotalLabel, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("专注批量推送的小而美的工具");
        aboutPanel.add(label2, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Proudly by RememBerBer 周波");
        aboutPanel.add(label3, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
