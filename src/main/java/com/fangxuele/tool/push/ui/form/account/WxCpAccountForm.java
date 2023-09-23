package com.fangxuele.tool.push.ui.form.account;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.WxCpAccountConfig;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.logic.msgsender.WxCpMsgSender;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.fangxuele.tool.push.util.UIUtil;
import com.fangxuele.tool.push.util.UndoUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;

@Getter
@Slf4j
public class WxCpAccountForm implements IAccountForm {
    private JPanel mainPanel;
    private JTextField corpIdTextField;
    private JTextField appNameTextField;
    private JTextField agentIdTextField;
    private JTextField secretTextField;
    private JCheckBox privateDepCheckBox;
    private JTextField baseApiUrlTextField;
    private JLabel baseApiUrlLabel;

    private static WxCpAccountForm wxCpAccountForm;

    public volatile static WxCpDefaultConfigImpl wxCpConfigStorage;
    public volatile static WxCpService wxCpService;

    @Override
    public void init(String accountName) {
        if (StringUtils.isNotEmpty(accountName)) {
            TAccount tAccount = accountMapper.selectByMsgTypeAndAccountName(App.config.getMsgType(), accountName);

            WxCpAccountForm instance = getInstance();
            WxCpAccountConfig wxCpAccountConfig = JSONUtil.toBean(tAccount.getAccountConfig(), WxCpAccountConfig.class);
            instance.getCorpIdTextField().setText(wxCpAccountConfig.getCorpId());
            instance.getAppNameTextField().setText(wxCpAccountConfig.getAppName());
            instance.getAgentIdTextField().setText(wxCpAccountConfig.getAgentId());
            instance.getSecretTextField().setText(wxCpAccountConfig.getSecret());
            instance.getPrivateDepCheckBox().setSelected(wxCpAccountConfig.getPrivateDep());
            instance.getBaseApiUrlTextField().setText(wxCpAccountConfig.getBaseApiUrl());
            if (wxCpAccountConfig.getPrivateDep()) {
                instance.getBaseApiUrlTextField().setVisible(true);
                instance.getBaseApiUrlLabel().setVisible(true);
            } else {
                instance.getBaseApiUrlTextField().setVisible(false);
                instance.getBaseApiUrlLabel().setVisible(false);
            }
        }
    }

    @Override
    public void save(String accountName) {
        if (StringUtils.isNotEmpty(accountName)) {
            TAccount tAccount = accountMapper.selectByMsgTypeAndAccountName(App.config.getMsgType(), accountName);
            WxCpAccountForm instance = getInstance();
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

                WxCpAccountConfig wxCpAccountConfig = new WxCpAccountConfig();
                wxCpAccountConfig.setCorpId(instance.getCorpIdTextField().getText());
                wxCpAccountConfig.setAppName(instance.getAppNameTextField().getText());
                wxCpAccountConfig.setAgentId(instance.getAgentIdTextField().getText());
                wxCpAccountConfig.setSecret(instance.getSecretTextField().getText());
                wxCpAccountConfig.setPrivateDep(instance.getPrivateDepCheckBox().isSelected());
                wxCpAccountConfig.setBaseApiUrl(instance.getBaseApiUrlTextField().getText());

                tAccount1.setAccountConfig(JSONUtil.toJsonStr(wxCpAccountConfig));

                tAccount1.setModifiedTime(now);

                if (existSameAccount) {
                    accountMapper.updateByMsgTypeAndAccountName(tAccount1);
                    WxCpMsgSender.removeAccount(tAccount1.getId());
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

    public static WxCpAccountForm getInstance() {
        if (wxCpAccountForm == null) {
            wxCpAccountForm = new WxCpAccountForm();
            wxCpAccountForm.getPrivateDepCheckBox().setSelected(false);
            wxCpAccountForm.getBaseApiUrlTextField().setVisible(false);
            wxCpAccountForm.getBaseApiUrlLabel().setVisible(false);
            wxCpAccountForm.getPrivateDepCheckBox().addChangeListener(e -> {
                if (wxCpAccountForm.getPrivateDepCheckBox().isSelected()) {
                    wxCpAccountForm.getBaseApiUrlTextField().setVisible(true);
                    wxCpAccountForm.getBaseApiUrlLabel().setVisible(true);
                } else {
                    wxCpAccountForm.getBaseApiUrlTextField().setVisible(false);
                    wxCpAccountForm.getBaseApiUrlLabel().setVisible(false);
                }
            });
        }
        UndoUtil.register(wxCpAccountForm);
        return wxCpAccountForm;
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
        panel1.setLayout(new GridLayoutManager(7, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("应用名称");
        panel1.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        appNameTextField = new JTextField();
        panel1.add(appNameTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("AgentId");
        panel1.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        agentIdTextField = new JTextField();
        panel1.add(agentIdTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Secret");
        panel1.add(label3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secretTextField = new JTextField();
        panel1.add(secretTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("企业ID");
        panel1.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        corpIdTextField = new JTextField();
        panel1.add(corpIdTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        privateDepCheckBox = new JCheckBox();
        privateDepCheckBox.setText("私有化部署");
        panel1.add(privateDepCheckBox, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        baseApiUrlTextField = new JTextField();
        panel1.add(baseApiUrlTextField, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        baseApiUrlLabel = new JLabel();
        baseApiUrlLabel.setText("私有BaseApiUrl");
        panel1.add(baseApiUrlLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
