package com.fangxuele.tool.push.ui.form.account;

import cn.hutool.json.JSONUtil;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.HttpClientConfig;
import com.aliyuncs.profile.DefaultProfile;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.AliYunAccountConfig;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgsender.AliYunMsgSender;
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
public class AliYunAccountForm implements IAccountForm {
    private JPanel mainPanel;
    private JTextField signTextField;
    private JTextField accessKeyIdTextField;
    private JTextField accessKeySecretTextField;

    private static AliYunAccountForm wxMpAccountForm;

    /**
     * 阿里云短信client
     */
    public volatile static IAcsClient iAcsClient;

    @Override
    public void init(String accountName) {
        if (StringUtils.isNotEmpty(accountName)) {
            TAccount tAccount = accountMapper.selectByMsgTypeAndAccountName(App.config.getMsgType(), accountName);

            AliYunAccountForm instance = getInstance();
            AliYunAccountConfig aliYunAccountConfig = JSONUtil.toBean(tAccount.getAccountConfig(), AliYunAccountConfig.class);
            instance.getAccessKeyIdTextField().setText(aliYunAccountConfig.getAccessKeyId());
            instance.getAccessKeySecretTextField().setText(aliYunAccountConfig.getAccessKeySecret());
            instance.getSignTextField().setText(aliYunAccountConfig.getSign());
        }
    }

    @Override
    public void save(String accountName) {
        if (StringUtils.isNotEmpty(accountName)) {
            TAccount tAccount = accountMapper.selectByMsgTypeAndAccountName(App.config.getMsgType(), accountName);
            AliYunAccountForm instance = getInstance();
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

                AliYunAccountConfig aliYunAccountConfig = new AliYunAccountConfig();
                aliYunAccountConfig.setAccessKeyId(instance.getAccessKeyIdTextField().getText());
                aliYunAccountConfig.setAccessKeySecret(instance.getAccessKeySecretTextField().getText());
                aliYunAccountConfig.setSign(instance.getSignTextField().getText());

                tAccount1.setAccountConfig(JSONUtil.toJsonStr(aliYunAccountConfig));

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

    public static AliYunAccountForm getInstance() {
        if (wxMpAccountForm == null) {
            wxMpAccountForm = new AliYunAccountForm();
        }
        UndoUtil.register(wxMpAccountForm);
        return wxMpAccountForm;
    }

    /**
     * 获取阿里云短信发送客户端
     *
     * @return IAcsClient
     */
    private static IAcsClient getAliyunIAcsClient(String accountName) {
        invalidAccount();

        if (iAcsClient == null) {
            synchronized (AliYunMsgSender.class) {
                if (iAcsClient == null) {
                    TAccount tAccount = accountMapper.selectByMsgTypeAndAccountName(MessageTypeEnum.getMsgTypeForAccount(), accountName);
                    if (tAccount == null) {
                        log.error("未获取到对应的微信公众号账号配置:{}", accountName);
                    }

                    AliYunAccountConfig aliYunAccountConfig = JSONUtil.toBean(tAccount.getAccountConfig(), AliYunAccountConfig.class);

                    String aliyunAccessKeyId = aliYunAccountConfig.getAccessKeyId();
                    String aliyunAccessKeySecret = aliYunAccountConfig.getAccessKeySecret();

                    // 创建DefaultAcsClient实例并初始化
                    DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", aliyunAccessKeyId, aliyunAccessKeySecret);

                    // 多个SDK client共享一个连接池，此处设置该连接池的参数，
                    // 比如每个host的最大连接数，超时时间等
                    HttpClientConfig clientConfig = HttpClientConfig.getDefault();
                    clientConfig.setMaxRequestsPerHost(App.config.getMaxThreads());
                    clientConfig.setConnectionTimeoutMillis(10000L);

                    profile.setHttpClientConfig(clientConfig);
                    iAcsClient = new DefaultAcsClient(profile);
                }
            }
        }
        return iAcsClient;
    }

    public static void invalidAccount() {
        iAcsClient = null;
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
        panel1.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("AccessKeyId");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("AccessKeySecret");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("短信签名");
        panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        signTextField = new JTextField();
        panel1.add(signTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        accessKeyIdTextField = new JTextField();
        panel1.add(accessKeyIdTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(300, -1), new Dimension(300, -1), null, 0, false));
        accessKeySecretTextField = new JTextField();
        panel1.add(accessKeySecretTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
