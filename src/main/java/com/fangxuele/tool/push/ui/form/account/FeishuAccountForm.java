package com.fangxuele.tool.push.ui.form.account;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.FeishuAccountConfig;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.logic.msgsender.FeishuMsgSender;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.util.FeishuBotSupport;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.fangxuele.tool.push.util.UndoUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;

/**
 * 飞书群自定义机器人账号配置界面。
 */
public class FeishuAccountForm implements IAccountForm {
    private final JPanel mainPanel = new JPanel(new GridBagLayout());
    private final JTextField webhookTextField = new JTextField();
    private final JPasswordField secretField = new JPasswordField();
    private final JTextField keywordTextField = new JTextField();

    private static FeishuAccountForm instance;

    private FeishuAccountForm() {
        GridBagConstraints label = constraints(0, 0, 0, GridBagConstraints.HORIZONTAL);
        label.anchor = GridBagConstraints.WEST;
        label.insets = new Insets(5, 5, 5, 12);
        GridBagConstraints field = constraints(1, 0, 1, GridBagConstraints.HORIZONTAL);
        field.insets = new Insets(5, 0, 5, 5);

        mainPanel.add(new JLabel("Webhook *"), label);
        webhookTextField.setToolTipText("https://open.feishu.cn/open-apis/bot/v2/hook/...");
        mainPanel.add(webhookTextField, field);

        label.gridy = 1;
        field.gridy = 1;
        mainPanel.add(new JLabel("签名密钥"), label);
        secretField.setToolTipText("飞书机器人安全设置中的签名密钥；未启用签名校验时留空");
        mainPanel.add(secretField, field);

        label.gridy = 2;
        field.gridy = 2;
        mainPanel.add(new JLabel("固定关键词"), label);
        keywordTextField.setToolTipText("可选；文本和富文本消息发送时自动添加为前缀");
        mainPanel.add(keywordTextField, field);

        GridBagConstraints tips = constraints(0, 3, 1, GridBagConstraints.HORIZONTAL);
        tips.gridwidth = 2;
        tips.insets = new Insets(12, 5, 5, 5);
        mainPanel.add(new JLabel("<html>在飞书群设置中添加“自定义机器人”后复制 Webhook。"
                + "支持签名校验；固定关键词仅自动应用于文本和富文本，卡片/原始 JSON 请自行包含关键词。</html>"), tips);

        GridBagConstraints spacer = constraints(0, 4, 1, GridBagConstraints.BOTH);
        spacer.gridwidth = 2;
        spacer.weighty = 1;
        mainPanel.add(Box.createVerticalGlue(), spacer);
    }

    public static FeishuAccountForm getInstance() {
        if (instance == null) {
            instance = new FeishuAccountForm();
            UndoUtil.register(instance);
        }
        return instance;
    }

    @Override
    public void init(String accountName) {
        if (StringUtils.isBlank(accountName)) {
            return;
        }
        TAccount account = accountMapper.selectByMsgTypeAndAccountName(App.config.getMsgType(), accountName);
        if (account == null) {
            return;
        }
        FeishuAccountConfig config = JSONUtil.toBean(account.getAccountConfig(), FeishuAccountConfig.class);
        webhookTextField.setText(StringUtils.defaultString(config.getWebhook()));
        secretField.setText(StringUtils.defaultString(config.getSecret()));
        keywordTextField.setText(StringUtils.defaultString(config.getKeyword()));
    }

    @Override
    public void save(String accountName) {
        if (StringUtils.isBlank(accountName)) {
            return;
        }
        String webhook = webhookTextField.getText().trim();
        FeishuBotSupport.validateWebhook(webhook);

        int msgType = App.config.getMsgType();
        TAccount existing = accountMapper.selectByMsgTypeAndAccountName(msgType, accountName);
        int cover = JOptionPane.NO_OPTION;
        if (existing != null) {
            cover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(),
                    "已经存在同名的账号，\n是否覆盖？", "确认", JOptionPane.YES_NO_OPTION);
        }
        if (existing != null && cover != JOptionPane.YES_OPTION) {
            return;
        }

        FeishuAccountConfig config = new FeishuAccountConfig();
        config.setWebhook(webhook);
        config.setSecret(new String(secretField.getPassword()).trim());
        config.setKeyword(keywordTextField.getText().trim());

        String now = SqliteUtil.nowDateForSqlite();
        TAccount account = new TAccount();
        account.setMsgType(msgType);
        account.setAccountName(accountName);
        account.setAccountConfig(JSONUtil.toJsonStr(config));
        account.setModifiedTime(now);
        if (existing != null) {
            FeishuAccountConfig oldConfig = JSONUtil.toBean(existing.getAccountConfig(), FeishuAccountConfig.class);
            accountMapper.updateByMsgTypeAndAccountName(account);
            FeishuMsgSender.removeWebhook(oldConfig.getWebhook());
            FeishuMsgSender.removeAccount(existing.getId());
        } else {
            account.setCreateTime(now);
            accountMapper.insertSelective(account);
        }

        JOptionPane.showMessageDialog(MainWindow.getInstance().getMainPanel(), "保存成功！", "成功",
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void clear() {
        webhookTextField.setText("");
        secretField.setText("");
        keywordTextField.setText("");
    }

    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }

    private static GridBagConstraints constraints(int x, int y, double weightX, int fill) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.weightx = weightX;
        constraints.fill = fill;
        return constraints;
    }
}
