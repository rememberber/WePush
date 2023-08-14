package com.fangxuele.tool.push.ui.form.account;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.EmailAccountConfig;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.ui.dialog.MailTestDialog;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.fangxuele.tool.push.util.UIUtil;
import com.fangxuele.tool.push.util.UndoUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;

@Getter
public class EmailAccountForm implements IAccountForm {
    private JPanel mainPanel;
    private JCheckBox mailStartTLSCheckBox;
    private JCheckBox mailSSLCheckBox;
    private JTextField mailHostTextField;
    private JTextField mailPortTextField;
    private JTextField mailFromTextField;
    private JTextField mailUserTextField;
    private JButton testMailButton;
    private JTextField mailPasswordField;

    private static EmailAccountForm emailAccountForm;

    @Override
    public void init(String accountName) {
        if (StringUtils.isNotEmpty(accountName)) {
            TAccount tAccount = accountMapper.selectByMsgTypeAndAccountName(App.config.getMsgType(), accountName);

            EmailAccountForm instance = getInstance();
            EmailAccountConfig emailAccountConfig = JSONUtil.toBean(tAccount.getAccountConfig(), EmailAccountConfig.class);

            instance.getMailStartTLSCheckBox().setSelected(emailAccountConfig.isMailStartTLS());
            instance.getMailSSLCheckBox().setSelected(emailAccountConfig.isMailSSL());
            instance.getMailHostTextField().setText(emailAccountConfig.getMailHost());
            instance.getMailPortTextField().setText(emailAccountConfig.getMailPort());
            instance.getMailFromTextField().setText(emailAccountConfig.getMailFrom());
            instance.getMailUserTextField().setText(emailAccountConfig.getMailUser());
            instance.getMailPasswordField().setText(emailAccountConfig.getMailPassword());
        }
    }

    @Override
    public void save(String accountName) {
        if (StringUtils.isNotEmpty(accountName)) {
            TAccount tAccount = accountMapper.selectByMsgTypeAndAccountName(App.config.getMsgType(), accountName);
            EmailAccountForm instance = getInstance();
            int msgType = App.config.getMsgType();

            boolean existSameAccount = false;

            if (tAccount != null) {
                existSameAccount = true;
            }

            int isCover = JOptionPane.NO_OPTION;
            if (existSameAccount) {
                // 如果存在，是否覆盖
                isCover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(), "已经存在同名的账号，\n是否覆盖？", "确认",
                        JOptionPane.YES_NO_OPTION);
            }

            if (!existSameAccount || isCover == JOptionPane.YES_OPTION) {

                String now = SqliteUtil.nowDateForSqlite();

                TAccount tAccount1 = new TAccount();
                tAccount1.setMsgType(msgType);
                tAccount1.setAccountName(accountName);

                EmailAccountConfig emailAccountConfig = new EmailAccountConfig();
                emailAccountConfig.setMailStartTLS(instance.getMailStartTLSCheckBox().isSelected());
                emailAccountConfig.setMailSSL(instance.getMailSSLCheckBox().isSelected());
                emailAccountConfig.setMailHost(instance.getMailHostTextField().getText());
                emailAccountConfig.setMailPort(instance.getMailPortTextField().getText());
                emailAccountConfig.setMailFrom(instance.getMailFromTextField().getText());
                emailAccountConfig.setMailUser(instance.getMailUserTextField().getText());
                emailAccountConfig.setMailPassword(instance.getMailPasswordField().getText());

                tAccount1.setAccountConfig(JSONUtil.toJsonStr(emailAccountConfig));

                tAccount1.setModifiedTime(now);

                if (existSameAccount) {
                    accountMapper.updateByMsgTypeAndAccountName(tAccount1);
                } else {
                    tAccount1.setCreateTime(now);
                    accountMapper.insertSelective(tAccount1);
                }

                JOptionPane.showMessageDialog(MainWindow.getInstance().getMainPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        }
    }

    @Override
    public void clear() {
        UIUtil.clearForm(getInstance());
    }

    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }

    public static EmailAccountForm getInstance() {
        if (emailAccountForm == null) {
            emailAccountForm = new EmailAccountForm();

            // E-Mail测试
            emailAccountForm.getTestMailButton().addActionListener(e -> {

                EmailAccountConfig emailAccountConfig = new EmailAccountConfig();
                emailAccountConfig.setMailHost(emailAccountForm.getMailHostTextField().getText());
                emailAccountConfig.setMailPort(emailAccountForm.getMailPortTextField().getText());
                emailAccountConfig.setMailFrom(emailAccountForm.getMailFromTextField().getText());
                emailAccountConfig.setMailUser(emailAccountForm.getMailUserTextField().getText());
                emailAccountConfig.setMailPassword(emailAccountForm.getMailPasswordField().getText());
                emailAccountConfig.setMailStartTLS(emailAccountForm.getMailStartTLSCheckBox().isSelected());
                emailAccountConfig.setMailSSL(emailAccountForm.getMailSSLCheckBox().isSelected());

                MailTestDialog mailTestDialog = new MailTestDialog(emailAccountConfig);
                mailTestDialog.pack();
                mailTestDialog.setVisible(true);
            });
        }
        UndoUtil.register(emailAccountForm);
        return emailAccountForm;
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 5, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(9, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(8, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("邮件服务器的SMTP地址");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("邮件服务器的SMTP端口");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("发件人（邮箱地址）");
        panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("用户名");
        label4.setToolTipText("如果使用foxmail邮箱，此处为qq号");
        panel1.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("密码");
        panel1.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(7, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel2.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        testMailButton = new JButton();
        testMailButton.setIcon(new ImageIcon(getClass().getResource("/icon/arrow_right.png")));
        testMailButton.setText("测试");
        panel2.add(testMailButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mailStartTLSCheckBox = new JCheckBox();
        mailStartTLSCheckBox.setText("使用STARTTLS安全连接");
        panel1.add(mailStartTLSCheckBox, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mailSSLCheckBox = new JCheckBox();
        mailSSLCheckBox.setText("使用SSL安全连接");
        panel1.add(mailSSLCheckBox, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mailHostTextField = new JTextField();
        panel1.add(mailHostTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        mailPortTextField = new JTextField();
        panel1.add(mailPortTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        mailFromTextField = new JTextField();
        panel1.add(mailFromTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        mailUserTextField = new JTextField();
        panel1.add(mailUserTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        mailPasswordField = new JTextField();
        panel1.add(mailPasswordField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
