package com.fangxuele.tool.push.ui.form.account;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.bean.account.KsYunAccountConfig;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgsender.KsYunMsgSender;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.fangxuele.tool.push.util.UndoUtil;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/** 金山云短信账号编辑器。 */
@Getter
public final class KsYunAccountForm implements IAccountForm {
    private static KsYunAccountForm instance;
    private final JPanel mainPanel = new JPanel(new GridBagLayout());
    private final JTextField accessKeyIdTextField = new JTextField(30);
    private final JPasswordField accessKeySecretTextField = new JPasswordField(30);
    private final JTextField signTextField = new JTextField(30);
    private final JComboBox<String> regionComboBox = new JComboBox<>(new String[]{"cn-beijing-6", "cn-guangzhou-1"});

    private KsYunAccountForm() {
        addRow(0, "AccessKey（AK） *", accessKeyIdTextField);
        addRow(1, "SecretKey（SK） *", accessKeySecretTextField);
        addRow(2, "已审核的短信签名 *", signTextField);
        addRow(3, "区域", regionComboBox);
        GridBagConstraints spacer = new GridBagConstraints();
        spacer.gridy = 4;
        spacer.weighty = 1;
        mainPanel.add(new JPanel(), spacer);
        UndoUtil.register(this);
    }

    private void addRow(int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(6, 5, 6, 5);
        c.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(field, c);
    }

    public static KsYunAccountForm getInstance() {
        if (instance == null) instance = new KsYunAccountForm();
        return instance;
    }

    @Override
    public void init(String accountName) {
        clear();
        if (accountName == null || accountName.isBlank()) return;
        TAccount saved = accountMapper.selectByMsgTypeAndAccountName(MessageTypeEnum.KS_YUN_CODE, accountName);
        if (saved == null) return;
        KsYunAccountConfig config = JSONUtil.toBean(saved.getAccountConfig(), KsYunAccountConfig.class);
        accessKeyIdTextField.setText(config.getAccessKeyId());
        accessKeySecretTextField.setText(config.getAccessKeySecret());
        signTextField.setText(config.getSign());
        regionComboBox.setSelectedItem(config.getRegion());
    }

    @Override
    public void save(String accountName) {
        if (accountName == null || accountName.isBlank()) return;
        KsYunAccountConfig config = new KsYunAccountConfig();
        config.setAccessKeyId(accessKeyIdTextField.getText().trim());
        config.setAccessKeySecret(new String(accessKeySecretTextField.getPassword()).trim());
        config.setSign(signTextField.getText().trim());
        config.setRegion((String) regionComboBox.getSelectedItem());
        try { config.validate(); }
        catch (IllegalArgumentException problem) {
            JOptionPane.showMessageDialog(mainPanel, problem.getMessage(), "配置错误", JOptionPane.WARNING_MESSAGE);
            return;
        }
        TAccount existing = accountMapper.selectByMsgTypeAndAccountName(MessageTypeEnum.KS_YUN_CODE, accountName);
        if (existing != null && JOptionPane.showConfirmDialog(mainPanel, "已经存在同名的账号，是否覆盖？", "确认",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        TAccount saved = new TAccount();
        saved.setMsgType(MessageTypeEnum.KS_YUN_CODE);
        saved.setAccountName(accountName);
        saved.setAccountConfig(JSONUtil.toJsonStr(config));
        saved.setModifiedTime(SqliteUtil.nowDateForSqlite());
        if (existing == null) {
            saved.setCreateTime(saved.getModifiedTime());
            accountMapper.insertSelective(saved);
        } else {
            accountMapper.updateByMsgTypeAndAccountName(saved);
            KsYunMsgSender.removeAccount(existing.getId());
        }
        JOptionPane.showMessageDialog(MainWindow.getInstance().getMainPanel(), "保存成功！", "成功",
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void clear() {
        accessKeyIdTextField.setText("");
        accessKeySecretTextField.setText("");
        signTextField.setText("");
        regionComboBox.setSelectedIndex(0);
    }
}
