package com.fangxuele.tool.push.ui.form.account;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.CarrierSmsAccountConfig;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.carriersms.CarrierSmsConnectionTestResult;
import com.fangxuele.tool.push.logic.carriersms.CarrierSmsConnectionTester;
import com.fangxuele.tool.push.logic.carriersms.CarrierSmsProtocol;
import com.fangxuele.tool.push.logic.msgsender.MsgSenderFactory;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.fangxuele.tool.push.util.UndoUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** CMPP/SMGP/SGIP/SMPP 统一账号表单。 */
public class CarrierSmsAccountForm implements IAccountForm {
    private static CarrierSmsAccountForm instance;

    private final JPanel mainPanel = new JPanel();
    private final JComboBox<CarrierSmsProtocol> protocolComboBox = new JComboBox<>(CarrierSmsProtocol.values());
    private final JTextField hostField = new JTextField();
    private final JTextField portField = new JTextField();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JTextField versionField = new JTextField();
    private final JTextField maxChannelsField = new JTextField();
    private final JTextField windowSizeField = new JTextField();
    private final JTextField timeoutField = new JTextField();
    private final JTextField heartbeatField = new JTextField();
    private final JButton testLoginButton = new JButton("测试登录（不发送短信）");
    private final JLabel testLoginResultLabel = new JLabel(" ");

    private final JTextField sourceAddressField = new JTextField();
    private final JTextField serviceIdField = new JTextField();
    private final JTextField msgSrcField = new JTextField();
    private final JTextField nodeIdField = new JTextField();
    private final JTextField corpIdField = new JTextField();
    private final JTextField systemTypeField = new JTextField();
    private final JCheckBox addZeroByteCheckBox = new JCheckBox("SMPP short_message 末尾追加 0 字节");

    private final JTextField chargeNumberField = new JTextField();
    private final JTextField feeTypeField = new JTextField();
    private final JTextField feeCodeField = new JTextField();
    private final JTextField feeValueField = new JTextField();
    private final JTextField fixedFeeField = new JTextField();
    private final JTextField sourceTonField = new JTextField();
    private final JTextField sourceNpiField = new JTextField();
    private final JTextField destinationTonField = new JTextField();
    private final JTextField destinationNpiField = new JTextField();
    private final Map<Component, EnumSet<CarrierSmsProtocol>> protocolVisibility = new LinkedHashMap<>();

    private CarrierSmsAccountForm() {
        buildUi();
        protocolComboBox.addActionListener(e -> protocolChanged());
        testLoginButton.addActionListener(e -> testLogin());
    }

    public static CarrierSmsAccountForm getInstance() {
        if (instance == null) {
            instance = new CarrierSmsAccountForm();
        }
        UndoUtil.register(instance);
        return instance;
    }

    private void buildUi() {
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        JPanel connection = section("连接与鉴权");
        addRow(connection, 0, "协议 *", protocolComboBox, "在同一消息类型下选择 CMPP、SMGP、SGIP 或 SMPP");
        addRow(connection, 1, "网关地址 *", hostField, "IP 或域名，不要填协议前缀");
        addRow(connection, 2, "网关端口 *", portField, "以运营商/网关商下发参数为准");
        addRow(connection, 3, "登录账号 *", usernameField, null);
        addRow(connection, 4, "登录密码 *", passwordField, null);
        addRow(connection, 5, "协议版本 *", versionField, "CMPP: 2.0/3.0，SMGP: 3.0，SGIP: 1.2，SMPP: 3.4");
        addRow(connection, 6, "TCP 连接数 *", maxChannelsField, "每个账号长连接池的最大连接数");
        addRow(connection, 7, "发送窗口 *", windowSizeField, "每条连接可同时等待的未应答请求数");
        addRow(connection, 8, "应答超时(ms) *", timeoutField, null);
        addRow(connection, 9, "心跳间隔(s) *", heartbeatField, "连接空闲时发送协议心跳的间隔，建议 30 秒");
        JPanel testPanel = new JPanel(new BorderLayout(8, 0));
        testPanel.add(testLoginButton, BorderLayout.WEST);
        testPanel.add(testLoginResultLabel, BorderLayout.CENTER);
        addFullRow(connection, 10, testPanel);
        connection.setMaximumSize(new Dimension(Integer.MAX_VALUE, connection.getPreferredSize().height));
        mainPanel.add(connection);

        JPanel submit = section("提交参数");
        addRow(submit, 0, "接入号/源地址 *", sourceAddressField, "CMPP Src_Id / SMGP SrcTermID / SGIP SPNumber / SMPP source_addr");
        addRow(submit, 1, "业务代码", serviceIdField, "CMPP/SMGP ServiceId，SGIP ServiceType，SMPP service_type");
        showFor(addRow(submit, 2, "企业代码 MsgSrc", msgSrcField, "CMPP 必填；SMGP 可选 TLV"),
                CarrierSmsProtocol.CMPP, CarrierSmsProtocol.SMGP);
        showFor(addRow(submit, 3, "SGIP NodeId *", nodeIdField, "1-4294967295"), CarrierSmsProtocol.SGIP);
        showFor(addRow(submit, 4, "SGIP CorpId *", corpIdField, "最长 5 个字符"), CarrierSmsProtocol.SGIP);
        showFor(addRow(submit, 5, "SMPP SystemType", systemTypeField, null), CarrierSmsProtocol.SMPP);
        showFor(addFullRow(submit, 6, addZeroByteCheckBox), CarrierSmsProtocol.SMPP);
        submit.setMaximumSize(new Dimension(Integer.MAX_VALUE, submit.getPreferredSize().height));
        mainPanel.add(submit);

        JPanel advanced = section("计费与 SMPP 地址参数（按网关要求填写）");
        showFor(addRow(advanced, 0, "SGIP ChargeNumber", chargeNumberField, null), CarrierSmsProtocol.SGIP);
        showFor(addRow(advanced, 1, "FeeType", feeTypeField, "CMPP/SMGP 为字符码，SGIP 为数字"),
                CarrierSmsProtocol.CMPP, CarrierSmsProtocol.SMGP, CarrierSmsProtocol.SGIP);
        showFor(addRow(advanced, 2, "FeeCode", feeCodeField, "CMPP/SMGP"),
                CarrierSmsProtocol.CMPP, CarrierSmsProtocol.SMGP);
        showFor(addRow(advanced, 3, "SGIP FeeValue", feeValueField, null), CarrierSmsProtocol.SGIP);
        showFor(addRow(advanced, 4, "SMGP FixedFee", fixedFeeField, null), CarrierSmsProtocol.SMGP);
        showFor(addRow(advanced, 5, "SMPP 源 TON / NPI", pair(sourceTonField, sourceNpiField), "0-255"),
                CarrierSmsProtocol.SMPP);
        showFor(addRow(advanced, 6, "SMPP 目标 TON / NPI", pair(destinationTonField, destinationNpiField), "0-255"),
                CarrierSmsProtocol.SMPP);
        advanced.setMaximumSize(new Dimension(Integer.MAX_VALUE, advanced.getPreferredSize().height));
        mainPanel.add(advanced);
        mainPanel.add(Box.createVerticalGlue());
        updateProtocolVisibility();
    }

    private static JPanel section(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title,
                TitledBorder.LEFT, TitledBorder.TOP));
        return panel;
    }

    private static JPanel pair(JComponent first, JComponent second) {
        JPanel pair = new JPanel(new GridLayout(1, 2, 8, 0));
        pair.add(first);
        pair.add(second);
        return pair;
    }

    private static List<Component> addRow(JPanel panel, int row, String labelText, JComponent component, String tooltip) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(4, 8, 4, 12);
        JLabel label = new JLabel(labelText);
        label.setLabelFor(component);
        label.setToolTipText(tooltip);
        panel.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(4, 0, 4, 8);
        component.setToolTipText(tooltip);
        component.setPreferredSize(new Dimension(420, component.getPreferredSize().height));
        panel.add(component, fieldConstraints);
        return List.of(label, component);
    }

    private static List<Component> addFullRow(JPanel panel, int row, JComponent component) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(4, 0, 4, 8);
        panel.add(component, constraints);
        return List.of(component);
    }

    private void showFor(List<Component> components, CarrierSmsProtocol... protocols) {
        EnumSet<CarrierSmsProtocol> visibleFor = EnumSet.noneOf(CarrierSmsProtocol.class);
        visibleFor.addAll(List.of(protocols));
        components.forEach(component -> protocolVisibility.put(component, visibleFor));
    }

    private void protocolChanged() {
        applyProtocolDefaults();
        updateProtocolVisibility();
        testLoginResultLabel.setText(" ");
    }

    private void updateProtocolVisibility() {
        CarrierSmsProtocol protocol = selectedProtocol();
        protocolVisibility.forEach((component, protocols) -> component.setVisible(protocols.contains(protocol)));
        for (Component component : mainPanel.getComponents()) {
            if (component instanceof JPanel section) {
                section.setMaximumSize(new Dimension(Integer.MAX_VALUE, section.getPreferredSize().height));
            }
        }
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void applyProtocolDefaults() {
        CarrierSmsProtocol protocol = selectedProtocol();
        if (StringUtils.isBlank(portField.getText()) || isDefaultPort(portField.getText())) {
            portField.setText(String.valueOf(protocol.getDefaultPort()));
        }
        if (StringUtils.isBlank(versionField.getText()) || isKnownVersion(versionField.getText())) {
            versionField.setText(protocol.getDefaultVersion());
        }
    }

    private void testLogin() {
        final CarrierSmsAccountConfig config;
        try {
            config = readConfig();
            List<String> errors = config.validate();
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(String.join("\n", errors));
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(mainPanel, e.getMessage(), "参数错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        testLoginButton.setEnabled(false);
        testLoginResultLabel.setText("正在连接并登录……");
        new SwingWorker<CarrierSmsConnectionTestResult, Void>() {
            @Override
            protected CarrierSmsConnectionTestResult doInBackground() {
                return CarrierSmsConnectionTester.test(config);
            }

            @Override
            protected void done() {
                testLoginButton.setEnabled(true);
                try {
                    CarrierSmsConnectionTestResult result = get();
                    testLoginResultLabel.setText(result.info() + "（" + result.elapsedMillis() + " ms）");
                    if (!result.success()) {
                        JOptionPane.showMessageDialog(mainPanel, result.info(), "测试登录失败", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    testLoginResultLabel.setText("测试已中断");
                } catch (ExecutionException e) {
                    testLoginResultLabel.setText("测试失败");
                    JOptionPane.showMessageDialog(mainPanel, "测试登录失败", "失败", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private static boolean isDefaultPort(String text) {
        for (CarrierSmsProtocol protocol : CarrierSmsProtocol.values()) {
            if (String.valueOf(protocol.getDefaultPort()).equals(text.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isKnownVersion(String text) {
        String value = text.trim();
        return value.equals("2.0") || value.equals("3.0") || value.equals("1.2") || value.equals("3.4");
    }

    private CarrierSmsProtocol selectedProtocol() {
        return CarrierSmsProtocol.from(protocolComboBox.getSelectedItem());
    }

    @Override
    public void init(String accountName) {
        if (StringUtils.isBlank(accountName)) {
            return;
        }
        TAccount account = accountMapper.selectByMsgTypeAndAccountName(MessageTypeEnum.CARRIER_SMS_CODE, accountName);
        if (account == null) {
            return;
        }
        CarrierSmsAccountConfig config = JSONUtil.toBean(account.getAccountConfig(), CarrierSmsAccountConfig.class);
        if (config == null) {
            return;
        }
        config.applyDefaults();
        protocolComboBox.setSelectedItem(config.getProtocol());
        hostField.setText(config.getHost());
        portField.setText(String.valueOf(config.getPort()));
        usernameField.setText(config.getUsername());
        passwordField.setText(config.getPassword());
        versionField.setText(config.getVersion());
        maxChannelsField.setText(String.valueOf(config.getMaxChannels()));
        windowSizeField.setText(String.valueOf(config.getWindowSize()));
        timeoutField.setText(String.valueOf(config.getRequestTimeoutMillis()));
        heartbeatField.setText(String.valueOf(config.getHeartbeatIntervalSeconds()));
        sourceAddressField.setText(config.getSourceAddress());
        serviceIdField.setText(config.getServiceId());
        msgSrcField.setText(config.getMsgSrc());
        nodeIdField.setText(String.valueOf(config.getNodeId()));
        corpIdField.setText(config.getCorpId());
        systemTypeField.setText(config.getSystemType());
        addZeroByteCheckBox.setSelected(config.isAddZeroByte());
        chargeNumberField.setText(config.getChargeNumber());
        feeTypeField.setText(config.getFeeType());
        feeCodeField.setText(config.getFeeCode());
        feeValueField.setText(config.getFeeValue());
        fixedFeeField.setText(config.getFixedFee());
        sourceTonField.setText(String.valueOf(config.getSourceTon()));
        sourceNpiField.setText(String.valueOf(config.getSourceNpi()));
        destinationTonField.setText(String.valueOf(config.getDestinationTon()));
        destinationNpiField.setText(String.valueOf(config.getDestinationNpi()));
        updateProtocolVisibility();
    }

    @Override
    public void save(String accountName) {
        CarrierSmsAccountConfig config = readConfig();
        List<String> errors = config.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        TAccount existing = accountMapper.selectByMsgTypeAndAccountName(MessageTypeEnum.CARRIER_SMS_CODE, accountName);
        if (existing != null) {
            int cover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(),
                    "已经存在同名的账号，\n是否覆盖？", "确认", JOptionPane.YES_NO_OPTION);
            if (cover != JOptionPane.YES_OPTION) {
                return;
            }
        }

        String now = SqliteUtil.nowDateForSqlite();
        TAccount account = new TAccount();
        account.setMsgType(MessageTypeEnum.CARRIER_SMS_CODE);
        account.setAccountName(accountName);
        account.setAccountConfig(JSONUtil.toJsonStr(config));
        account.setModifiedTime(now);
        if (existing == null) {
            account.setCreateTime(now);
            accountMapper.insertSelective(account);
        } else {
            accountMapper.updateByMsgTypeAndAccountName(account);
            MsgSenderFactory.removeAccount(MessageTypeEnum.CARRIER_SMS_CODE, existing.getId());
        }
        JOptionPane.showMessageDialog(MainWindow.getInstance().getMainPanel(), "保存成功！", "成功",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private CarrierSmsAccountConfig readConfig() {
        try {
            CarrierSmsAccountConfig config = new CarrierSmsAccountConfig();
            config.setProtocol(selectedProtocol());
            config.setHost(trim(hostField));
            config.setPort(integer(portField, "网关端口"));
            config.setUsername(trim(usernameField));
            config.setPassword(new String(passwordField.getPassword()));
            config.setVersion(trim(versionField));
            config.setMaxChannels(integer(maxChannelsField, "TCP 连接数"));
            config.setWindowSize(integer(windowSizeField, "发送窗口"));
            config.setRequestTimeoutMillis(integer(timeoutField, "应答超时"));
            config.setHeartbeatIntervalSeconds(integer(heartbeatField, "心跳间隔"));
            config.setSourceAddress(trim(sourceAddressField));
            config.setServiceId(trim(serviceIdField));
            config.setMsgSrc(trim(msgSrcField));
            config.setNodeId(longValue(nodeIdField, "SGIP NodeId"));
            config.setCorpId(trim(corpIdField));
            config.setSystemType(trim(systemTypeField));
            config.setAddZeroByte(addZeroByteCheckBox.isSelected());
            config.setChargeNumber(trim(chargeNumberField));
            config.setFeeType(trim(feeTypeField));
            config.setFeeCode(trim(feeCodeField));
            config.setFeeValue(trim(feeValueField));
            config.setFixedFee(trim(fixedFeeField));
            config.setSourceTon(integer(sourceTonField, "SMPP 源 TON"));
            config.setSourceNpi(integer(sourceNpiField, "SMPP 源 NPI"));
            config.setDestinationTon(integer(destinationTonField, "SMPP 目标 TON"));
            config.setDestinationNpi(integer(destinationNpiField, "SMPP 目标 NPI"));
            return config;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private static String trim(JTextField field) {
        return StringUtils.trimToEmpty(field.getText());
    }

    private static int integer(JTextField field, String name) {
        try {
            return Integer.parseInt(trim(field));
        } catch (NumberFormatException e) {
            throw new NumberFormatException(name + " 必须是整数");
        }
    }

    private static long longValue(JTextField field, String name) {
        String value = trim(field);
        if (value.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(name + " 必须是整数");
        }
    }

    @Override
    public void clear() {
        protocolComboBox.setSelectedItem(CarrierSmsProtocol.CMPP);
        hostField.setText("");
        portField.setText(String.valueOf(CarrierSmsProtocol.CMPP.getDefaultPort()));
        usernameField.setText("");
        passwordField.setText("");
        versionField.setText(CarrierSmsProtocol.CMPP.getDefaultVersion());
        maxChannelsField.setText("1");
        windowSizeField.setText("16");
        timeoutField.setText("10000");
        heartbeatField.setText("30");
        sourceAddressField.setText("");
        serviceIdField.setText("");
        msgSrcField.setText("");
        nodeIdField.setText("0");
        corpIdField.setText("");
        systemTypeField.setText("");
        addZeroByteCheckBox.setSelected(false);
        chargeNumberField.setText("000000000000000000000");
        feeTypeField.setText("");
        feeCodeField.setText("");
        feeValueField.setText("");
        fixedFeeField.setText("");
        sourceTonField.setText("0");
        sourceNpiField.setText("0");
        destinationTonField.setText("0");
        destinationNpiField.setText("1");
        testLoginResultLabel.setText(" ");
        updateProtocolVisibility();
    }

    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }
}
