package com.fangxuele.tool.push.ui.dialog;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.EmailAccountConfig;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.util.ComponentUtil;
import com.fangxuele.tool.push.util.HikariUtil;
import com.fangxuele.tool.push.util.ScrollUtil;
import com.fangxuele.tool.push.util.SystemUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.SystemInfo;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;

public class SettingDialog extends JDialog {
    private JPanel contentPane;
    private JScrollPane settingScrollPane;
    private JCheckBox autoCheckUpdateCheckBox;
    private JCheckBox useTrayCheckBox;
    private JCheckBox closeToTrayCheckBox;
    private JCheckBox defaultMaxWindowCheckBox;
    private JTextField maxThreadsTextField;
    private JButton maxThreadsSaveButton;
    private JButton saveMailButton;
    private JButton testMailButton;
    private JCheckBox mailStartTLSCheckBox;
    private JCheckBox mailSSLCheckBox;
    private JTextField mailHostTextField;
    private JTextField mailPortTextField;
    private JTextField mailFromTextField;
    private JTextField mailUserTextField;
    private JPasswordField mailPasswordField;
    private JTextField mysqlUrlTextField;
    private JTextField mysqlUserTextField;
    private JPasswordField mysqlPasswordField;
    private JButton settingTestDbLinkButton;
    private JButton settingDbInfoSaveButton;

    private static final Log logger = LogFactory.get();

    public SettingDialog() {
        super(App.mainFrame, "设置");
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
        ScrollUtil.smoothPane(settingScrollPane);

        // 常规
        autoCheckUpdateCheckBox.setSelected(App.config.isAutoCheckUpdate());
        useTrayCheckBox.setSelected(App.config.isUseTray());
        closeToTrayCheckBox.setSelected(App.config.isCloseToTray());
        defaultMaxWindowCheckBox.setSelected(App.config.isDefaultMaxWindow());
        maxThreadsTextField.setText(String.valueOf(App.config.getMaxThreads()));

        // E-Mail
        mailHostTextField.setText(App.config.getMailHost());
        mailPortTextField.setText(App.config.getMailPort());
        mailFromTextField.setText(App.config.getMailFrom());
        mailUserTextField.setText(App.config.getMailUser());
        mailPasswordField.setText(App.config.getMailPassword());
        mailStartTLSCheckBox.setSelected(App.config.isMailUseStartTLS());
        mailSSLCheckBox.setSelected(App.config.isMailUseSSL());

        // MySQL
        mysqlUrlTextField.setText(App.config.getMysqlUrl());
        mysqlUserTextField.setText(App.config.getMysqlUser());
        mysqlPasswordField.setText(App.config.getMysqlPassword());

        saveMailButton.setIcon(new FlatSVGIcon("icon/save.svg"));
        settingDbInfoSaveButton.setIcon(new FlatSVGIcon("icon/save.svg"));
        maxThreadsSaveButton.setIcon(new FlatSVGIcon("icon/save.svg"));
        testMailButton.setIcon(new FlatSVGIcon("icon/test.svg"));
        settingTestDbLinkButton.setIcon(new FlatSVGIcon("icon/test.svg"));

        // 监听事件
        // 设置-常规-启动时自动检查更新
        autoCheckUpdateCheckBox.addActionListener(e -> {
            App.config.setAutoCheckUpdate(autoCheckUpdateCheckBox.isSelected());
            App.config.save();
        });
        // 设置-常规-显示系统托盘图标
        useTrayCheckBox.addActionListener(e -> {
            App.config.setUseTray(useTrayCheckBox.isSelected());
            App.config.save();
            if (App.tray == null && App.config.isUseTray()) {
                Init.initTray();
            } else if (App.tray != null && !App.config.isUseTray()) {
                App.tray.remove(App.trayIcon);
                App.trayIcon = null;
                App.tray = null;
            }
        });
        // 设置-常规-关闭窗口时最小化到系统托盘
        closeToTrayCheckBox.addActionListener(e -> {
            App.config.setCloseToTray(closeToTrayCheckBox.isSelected());
            App.config.save();
        });
        // 设置-常规-默认最大化窗口
        defaultMaxWindowCheckBox.addActionListener(e -> {
            App.config.setDefaultMaxWindow(defaultMaxWindowCheckBox.isSelected());
            App.config.save();
        });

        // 设置-常规-最大线程数
        maxThreadsSaveButton.addActionListener(e -> {
            try {
                App.config.setMaxThreads(Integer.valueOf(maxThreadsTextField.getText()));
                App.config.save();
//                PushListener.refreshPushInfo();

                JOptionPane.showMessageDialog(this, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(this, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // E-Mail测试
        testMailButton.addActionListener(e -> {
            App.config.setMailHost(mailHostTextField.getText());
            App.config.setMailPort(mailPortTextField.getText());
            App.config.setMailFrom(mailFromTextField.getText());
            App.config.setMailUser(mailUserTextField.getText());
            App.config.setMailPassword(new String(mailPasswordField.getPassword()));
            App.config.setMailUseStartTLS(mailStartTLSCheckBox.isSelected());
            App.config.setMailUseSSL(mailSSLCheckBox.isSelected());

            EmailAccountConfig emailAccountConfig = new EmailAccountConfig();
            emailAccountConfig.setMailHost(mailHostTextField.getText());
            emailAccountConfig.setMailPort(mailPortTextField.getText());
            emailAccountConfig.setMailFrom(mailFromTextField.getText());
            emailAccountConfig.setMailUser(mailUserTextField.getText());
            emailAccountConfig.setMailPassword(new String(mailPasswordField.getPassword()));
            emailAccountConfig.setMailStartTLS(mailStartTLSCheckBox.isSelected());
            emailAccountConfig.setMailSSL(mailSSLCheckBox.isSelected());

            MailTestDialog mailTestDialog = new MailTestDialog(emailAccountConfig);
            mailTestDialog.pack();
            mailTestDialog.setVisible(true);
        });

        // E-Mail保存
        saveMailButton.addActionListener(e -> {
            try {
                App.config.setMailHost(mailHostTextField.getText());
                App.config.setMailPort(mailPortTextField.getText());
                App.config.setMailFrom(mailFromTextField.getText());
                App.config.setMailUser(mailUserTextField.getText());
                App.config.setMailPassword(new String(mailPasswordField.getPassword()));
                App.config.setMailUseStartTLS(mailStartTLSCheckBox.isSelected());
                App.config.setMailUseSSL(mailSSLCheckBox.isSelected());
                App.config.save();

                JOptionPane.showMessageDialog(this, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(this, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // mysql数据库-测试链接
        settingTestDbLinkButton.addActionListener(e -> {
            HikariDataSource hikariDataSource = null;
            try {
                String dbUrl = mysqlUrlTextField.getText();
                String dbUser = mysqlUserTextField.getText();
                String dbPassword = new String(mysqlPasswordField.getPassword());
                if (StringUtils.isBlank(dbUrl)) {
                    mysqlUrlTextField.grabFocus();
                    return;
                }
                if (StringUtils.isBlank(dbUser)) {
                    mysqlUserTextField.grabFocus();
                    return;
                }
                if (StringUtils.isBlank(dbPassword)) {
                    mysqlPasswordField.grabFocus();
                    return;
                }
                hikariDataSource = new HikariDataSource();
                hikariDataSource.setJdbcUrl("jdbc:mysql://" + dbUrl);
                hikariDataSource.setUsername(dbUser);
                hikariDataSource.setPassword(dbPassword);
                if (hikariDataSource.getConnection() == null) {
                    JOptionPane.showMessageDialog(this, "连接失败", "失败",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "连接成功！", "成功",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(this, "连接失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            } finally {
                if (hikariDataSource != null) {
                    try {
                        hikariDataSource.close();
                    } catch (Exception e2) {
                        logger.error(e2);
                    }
                }
            }
        });

        // mysql数据库-保存
        settingDbInfoSaveButton.addActionListener(e -> {
            try {
                App.config.setMysqlUrl(mysqlUrlTextField.getText());
                App.config.setMysqlUser(mysqlUserTextField.getText());
                App.config.setMysqlPassword(new String(mysqlPasswordField.getPassword()));
                App.config.save();

                if (HikariUtil.getHikariDataSource() != null) {
                    HikariUtil.getHikariDataSource().close();
                }

                JOptionPane.showMessageDialog(this, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(this, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

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
        contentPane.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        settingScrollPane = new JScrollPane();
        panel1.add(settingScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        settingScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        panel2.setMaximumSize(new Dimension(-1, -1));
        panel2.setMinimumSize(new Dimension(-1, -1));
        settingScrollPane.setViewportView(panel2);
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 1, new Insets(20, 20, 0, 20), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(600, -1), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(5, 1, new Insets(15, 15, 10, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "常规", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel4.getFont()), null));
        autoCheckUpdateCheckBox = new JCheckBox();
        autoCheckUpdateCheckBox.setText("自动检查更新");
        panel4.add(autoCheckUpdateCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useTrayCheckBox = new JCheckBox();
        useTrayCheckBox.setText("显示系统托盘图标");
        panel4.add(useTrayCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        closeToTrayCheckBox = new JCheckBox();
        closeToTrayCheckBox.setText("关闭窗口时最小化到系统托盘");
        panel4.add(closeToTrayCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        defaultMaxWindowCheckBox = new JCheckBox();
        defaultMaxWindowCheckBox.setText("默认最大化窗口");
        panel4.add(defaultMaxWindowCheckBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("最大线程数");
        panel5.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maxThreadsTextField = new JTextField();
        panel5.add(maxThreadsTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(80, -1), null, 0, false));
        maxThreadsSaveButton = new JButton();
        maxThreadsSaveButton.setText("保存");
        panel5.add(maxThreadsSaveButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel5.add(spacer2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(8, 2, new Insets(15, 15, 10, 0), -1, -1));
        panel3.add(panel6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel6.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "E-Mail", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel6.getFont()), null));
        final JLabel label2 = new JLabel();
        label2.setText("邮件服务器的SMTP地址");
        panel6.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("邮件服务器的SMTP端口");
        panel6.add(label3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("发件人（邮箱地址）");
        panel6.add(label4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("用户名");
        label5.setToolTipText("如果使用foxmail邮箱，此处为qq号");
        panel6.add(label5, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("密码");
        panel6.add(label6, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel7, new GridConstraints(7, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        saveMailButton = new JButton();
        saveMailButton.setText("保存");
        panel7.add(saveMailButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel7.add(spacer3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        testMailButton = new JButton();
        testMailButton.setText("测试");
        panel7.add(testMailButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mailStartTLSCheckBox = new JCheckBox();
        mailStartTLSCheckBox.setText("使用STARTTLS安全连接");
        panel6.add(mailStartTLSCheckBox, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mailSSLCheckBox = new JCheckBox();
        mailSSLCheckBox.setText("使用SSL安全连接");
        panel6.add(mailSSLCheckBox, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mailHostTextField = new JTextField();
        panel6.add(mailHostTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        mailPortTextField = new JTextField();
        panel6.add(mailPortTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        mailFromTextField = new JTextField();
        panel6.add(mailFromTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        mailUserTextField = new JTextField();
        panel6.add(mailUserTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        mailPasswordField = new JPasswordField();
        panel6.add(mailPasswordField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(4, 2, new Insets(15, 15, 10, 0), -1, -1));
        panel3.add(panel8, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel8.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "MySQL数据库", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel8.getFont()), null));
        final JLabel label7 = new JLabel();
        label7.setText("数据库地址");
        panel8.add(label7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mysqlUrlTextField = new JTextField();
        panel8.add(mysqlUrlTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(300, -1), new Dimension(300, -1), null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("用户名");
        panel8.add(label8, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mysqlUserTextField = new JTextField();
        panel8.add(mysqlUserTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("密码");
        panel8.add(label9, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mysqlPasswordField = new JPasswordField();
        panel8.add(mysqlPasswordField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel9, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        settingTestDbLinkButton = new JButton();
        settingTestDbLinkButton.setText("测试连接");
        panel9.add(settingTestDbLinkButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel9.add(spacer4, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        settingDbInfoSaveButton = new JButton();
        settingDbInfoSaveButton.setText("保存");
        panel9.add(settingDbInfoSaveButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
