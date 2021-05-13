package com.fangxuele.tool.push.ui.form.account;

import cn.hutool.json.JSONUtil;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.sms.SmsClient;
import com.baidubce.services.sms.SmsClientConfiguration;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.BdYunAccountConfig;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgsender.BdYunMsgSender;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.fangxuele.tool.push.util.UIUtil;
import com.fangxuele.tool.push.util.UndoUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;

@Getter
@Slf4j
public class BdYunAccountForm implements IAccountForm {
    private JPanel mainPanel;
    private JTextField bdEndPointTextField;
    private JTextField bdInvokeIdTextField;
    private JTextField bdAccessKeyIdTextField;
    private JTextField bdSecretAccessKeyTextField;

    private static BdYunAccountForm wxMpAccountForm;

    /**
     * 百度云短信SmsClient
     */
    public volatile static SmsClient smsClient;

    @Override
    public void init(String accountName) {
        if (StringUtils.isNotEmpty(accountName)) {
            TAccount tAccount = accountMapper.selectByMsgTypeAndAccountName(App.config.getMsgType(), accountName);

            BdYunAccountForm instance = getInstance();
            BdYunAccountConfig bdYunAccountConfig = JSONUtil.toBean(tAccount.getAccountConfig(), BdYunAccountConfig.class);
            instance.getBdEndPointTextField().setText(bdYunAccountConfig.getBdEndPoint());
            instance.getBdInvokeIdTextField().setText(bdYunAccountConfig.getBdInvokeId());
            instance.getBdSecretAccessKeyTextField().setText(bdYunAccountConfig.getBdSecretAccessKey());
            instance.getBdAccessKeyIdTextField().setText(bdYunAccountConfig.getBdAccessKeyId());
        }
    }

    @Override
    public void save(String accountName) {
        if (StringUtils.isNotEmpty(accountName)) {
            TAccount tAccount = accountMapper.selectByMsgTypeAndAccountName(App.config.getMsgType(), accountName);
            BdYunAccountForm instance = getInstance();
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
                tAccount1.setMsgType(String.valueOf(msgType));
                tAccount1.setAccountName(accountName);

                BdYunAccountConfig bdYunAccountConfig = new BdYunAccountConfig();
                bdYunAccountConfig.setBdEndPoint(instance.getBdEndPointTextField().getText());
                bdYunAccountConfig.setBdInvokeId(instance.getBdInvokeIdTextField().getText());
                bdYunAccountConfig.setBdSecretAccessKey(instance.getBdSecretAccessKeyTextField().getText());
                bdYunAccountConfig.setBdAccessKeyId(instance.getBdAccessKeyIdTextField().getText());

                tAccount1.setAccountConfig(JSONUtil.toJsonStr(bdYunAccountConfig));

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

    public static BdYunAccountForm getInstance() {
        if (wxMpAccountForm == null) {
            wxMpAccountForm = new BdYunAccountForm();
        }
        UndoUtil.register(wxMpAccountForm);
        return wxMpAccountForm;
    }

    /**
     * 获取百度云短信发送客户端
     *
     * @return SmsClient
     */
    private static SmsClient getBdYunSmsClient(String accountName) {
        invalidAccount();

        if (smsClient == null) {
            synchronized (BdYunMsgSender.class) {
                if (smsClient == null) {
                    TAccount tAccount = accountMapper.selectByMsgTypeAndAccountName(MessageTypeEnum.getMsgTypeForAccount(), accountName);
                    if (tAccount == null) {
                        log.error("未获取到对应的微信公众号账号配置:{}", accountName);
                    }

                    BdYunAccountConfig bdYunAccountConfig = JSONUtil.toBean(tAccount.getAccountConfig(), BdYunAccountConfig.class);

                    // SMS服务域名，可根据环境选择具体域名
                    String endPoint = bdYunAccountConfig.getBdEndPoint();
                    // 发送账号安全认证的Access Key ID
                    String accessKeyId = bdYunAccountConfig.getBdAccessKeyId();
                    // 发送账号安全认证的Secret Access Key
                    String secretAccessKy = bdYunAccountConfig.getBdSecretAccessKey();

                    // ak、sk等config
                    SmsClientConfiguration config = new SmsClientConfiguration();
                    config.setCredentials(new DefaultBceCredentials(accessKeyId, secretAccessKy));
                    config.setEndpoint(endPoint);

                    // 实例化发送客户端
                    smsClient = new SmsClient(config);
                }
            }
        }
        return smsClient;
    }

    public static void invalidAccount() {
        smsClient = null;
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
        panel1.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("AccessKeyID");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("SecretAccessKey");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("SMS服务域名");
        panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("签名的调用ID");
        panel1.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bdEndPointTextField = new JTextField();
        panel1.add(bdEndPointTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        bdInvokeIdTextField = new JTextField();
        panel1.add(bdInvokeIdTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        bdAccessKeyIdTextField = new JTextField();
        panel1.add(bdAccessKeyIdTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(300, -1), new Dimension(300, -1), null, 0, false));
        bdSecretAccessKeyTextField = new JTextField();
        panel1.add(bdSecretAccessKeyTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
