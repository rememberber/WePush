package com.fangxuele.tool.push.ui.dialog;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TPeopleMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TPeople;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.form.PeopleManageForm;
import com.fangxuele.tool.push.util.ComponentUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.fangxuele.tool.push.util.SystemUtil;
import com.formdev.flatlaf.util.SystemInfo;
import com.google.common.collect.Maps;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * DingAppDialog
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/9/5.
 */
public class NewPeopleDialog extends JDialog {
    private JPanel contentPane;
    private JTextField peopleNameTextField;
    private JButton saveButton;
    private JComboBox accountComboBox;
    private static Map<String, Integer> accountMap;

    private Log logger = LogFactory.get();
    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TPeopleMapper peopleMapper = MybatisUtil.getSqlSession().getMapper(TPeopleMapper.class);

    public NewPeopleDialog() {
        super(App.mainFrame, "新建人群");
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

        int msgType = App.config.getMsgType();

        initAccountComboBox(msgType);

        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.5, 0.5);

        // 保存按钮事件
        saveButton.addActionListener(e -> {
            try {
                String peopleName = peopleNameTextField.getText();
                if (StringUtils.isBlank(peopleName)) {
                    JOptionPane.showMessageDialog(this, "请填写人群名称！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                String selectedAccountName = (String) accountComboBox.getSelectedItem();
                if (StringUtils.isBlank(selectedAccountName)) {
                    JOptionPane.showMessageDialog(this, "请创建并选择账号！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                Integer selectedAccountId = accountMap.get(selectedAccountName);
                if (selectedAccountId == null) {
                    logger.error("创建人群失败:selectedAccountId为空");
                    return;
                }

                // 校验是否有同名人群
                TPeople tPeople = peopleMapper.selectByMsgTypeAndAccountIdAndName(String.valueOf(msgType), selectedAccountId, peopleName);
                if (tPeople != null) {
                    JOptionPane.showMessageDialog(this, "该人群名称已存在！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                TPeople tPeopleToSave = new TPeople();
                tPeopleToSave.setMsgType(msgType);
                tPeopleToSave.setAccountId(selectedAccountId);
                tPeopleToSave.setPeopleName(peopleName);
                tPeopleToSave.setAppVersion(UiConsts.APP_VERSION);
                String now = SqliteUtil.nowDateForSqlite();
                tPeopleToSave.setCreateTime(now);
                tPeopleToSave.setModifiedTime(now);

                peopleMapper.insert(tPeopleToSave);

                PeopleManageForm.initPeopleList();

                dispose();
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(this, "保存失败！\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
            }

        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void initAccountComboBox(int msgType) {
        accountMap = Maps.newHashMap();
        List<TAccount> tAccountList = accountMapper.selectByMsgType(msgType);
        for (TAccount tAccount : tAccountList) {
            String accountName = tAccount.getAccountName();
            Integer accountId = tAccount.getId();
            accountComboBox.addItem(accountName);
            accountMap.put(accountName, accountId);
        }
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    /**
     * 清空表单
     */
    public void clearFields() {
        peopleNameTextField.setText("");
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
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 10, 10), -1, -1));
        contentPane.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        saveButton = new JButton();
        saveButton.setText("保存");
        panel2.add(saveButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 2, new Insets(10, 10, 0, 10), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("人群名称");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        peopleNameTextField = new JTextField();
        panel3.add(peopleNameTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("账号");
        panel3.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        accountComboBox = new JComboBox();
        panel3.add(accountComboBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        contentPane.add(spacer2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
