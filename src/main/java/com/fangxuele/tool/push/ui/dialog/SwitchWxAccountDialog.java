package com.fangxuele.tool.push.ui.dialog;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TWxAccountMapper;
import com.fangxuele.tool.push.domain.TWxAccount;
import com.fangxuele.tool.push.ui.form.SettingForm;
import com.fangxuele.tool.push.ui.listener.SettingListener;
import com.fangxuele.tool.push.util.JTableUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * <pre>
 * 多账号管理dialog
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/5/23.
 */
public class SwitchWxAccountDialog extends JDialog {
    Log logger = LogFactory.get();
    private JPanel contentPane;
    private JButton buttonOk;
    private JTable accountsTable;
    private JTextField nameTextField;
    private JTextField appIdTextField;
    private JTextField appSecretTextField;
    private JTextField tokenTextField;
    private JTextField aesKeyTextField;
    private JButton addButton;
    private JButton deleteButton;

    private static TWxAccountMapper wxAccountMapper = MybatisUtil.getSqlSession().getMapper(TWxAccountMapper.class);

    public SwitchWxAccountDialog() {
        super(App.mainFrame, "多账号管理");
        String title = "多账号管理";
        if (SettingForm.WX_ACCOUNT_TYPE_MP.equals(SettingListener.wxAccountType)) {
            title = "多账号管理-公众号";
        } else if (SettingForm.WX_ACCOUNT_TYPE_MA.equals(SettingListener.wxAccountType)) {
            title = "多账号管理-小程序";
        }
        setTitle(title);
        setContentPane(contentPane);
        setModal(true);

        //得到屏幕的尺寸
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((int) (screenSize.width * 0.2), (int) (screenSize.height * 0.12), (int) (screenSize.width * 0.6),
                (int) (screenSize.height * 0.63));

        Dimension preferSize = new Dimension((int) (screenSize.width * 0.6),
                (int) (screenSize.height * 0.63));
        setPreferredSize(preferSize);

        buttonOk.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        addButton.addActionListener(e -> {
            String accountName = nameTextField.getText();
            if (StringUtils.isBlank(accountName)) {
                JOptionPane.showMessageDialog(this, "请填写账号名称！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            List<TWxAccount> tWxAccountList = wxAccountMapper.selectByAccountTypeAndAccountName(SettingListener.wxAccountType, accountName);
            if (tWxAccountList.size() > 0) {
                JOptionPane.showMessageDialog(this, "已存在同名账号，请重新命名！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            TWxAccount tWxAccount = new TWxAccount();
            String now = SqliteUtil.nowDateForSqlite();
            tWxAccount.setAccountType(SettingListener.wxAccountType);
            tWxAccount.setAccountName(accountName);
            tWxAccount.setAppId(appIdTextField.getText());
            tWxAccount.setAppSecret(appSecretTextField.getText());
            tWxAccount.setToken(tokenTextField.getText());
            tWxAccount.setAesKey(aesKeyTextField.getText());
            tWxAccount.setCreateTime(now);
            tWxAccount.setModifiedTime(now);

            wxAccountMapper.insert(tWxAccount);
            renderTable();
            SettingForm.initSwitchMultiAccount();
            JOptionPane.showMessageDialog(this, "添加成功！", "成功",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        deleteButton.addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                int[] selectedRows = accountsTable.getSelectedRows();
                if (selectedRows.length == 0) {
                    JOptionPane.showMessageDialog(this, "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    int isDelete = JOptionPane.showConfirmDialog(this, "确认删除？", "确认",
                            JOptionPane.YES_NO_OPTION);
                    if (isDelete == JOptionPane.YES_OPTION) {
                        DefaultTableModel tableModel = (DefaultTableModel) accountsTable.getModel();
                        for (int i = selectedRows.length; i > 0; i--) {
                            int selectedRow = accountsTable.getSelectedRow();
                            Integer selectedId = (Integer) tableModel.getValueAt(selectedRow, 0);
                            wxAccountMapper.deleteByPrimaryKey(selectedId);
                            tableModel.removeRow(selectedRow);
                        }
                        SettingForm.initSwitchMultiAccount();
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(this, "删除失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        }));
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    /**
     * 多账号表格
     */
    public void renderTable() {
        String[] headerNames = {"id", "账号名称", "AppId", "AppSecret", "Token", "AesKey"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        accountsTable.setModel(model);

        DefaultTableCellRenderer hr = (DefaultTableCellRenderer) accountsTable.getTableHeader()
                .getDefaultRenderer();
        // 表头列名居左
        hr.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);

        List<TWxAccount> wxAccountList = wxAccountMapper.selectByAccountType(SettingListener.wxAccountType);
        Object[] data;
        for (TWxAccount tWxAccount : wxAccountList) {
            data = new Object[6];
            data[0] = tWxAccount.getId();
            data[1] = tWxAccount.getAccountName();
            data[2] = tWxAccount.getAppId();
            data[3] = tWxAccount.getAppSecret();
            data[4] = tWxAccount.getToken();
            data[5] = tWxAccount.getAesKey();
            model.addRow(data);
        }

        // 隐藏id列
        JTableUtil.hideColumn(accountsTable, 0);
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
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOk = new JButton();
        buttonOk.setText("好了");
        panel2.add(buttonOk, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deleteButton = new JButton();
        deleteButton.setText("删除");
        panel2.add(deleteButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 6, new Insets(5, 5, 0, 5), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "添加新账号"));
        final JLabel label1 = new JLabel();
        label1.setText("名称");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nameTextField = new JTextField();
        panel3.add(nameTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("AppId");
        panel3.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        appIdTextField = new JTextField();
        panel3.add(appIdTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("AppSecret");
        panel3.add(label3, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        appSecretTextField = new JTextField();
        panel3.add(appSecretTextField, new GridConstraints(1, 4, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Token");
        panel3.add(label4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tokenTextField = new JTextField();
        panel3.add(tokenTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("AES Key");
        panel3.add(label5, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        aesKeyTextField = new JTextField();
        panel3.add(aesKeyTextField, new GridConstraints(2, 4, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(3, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        addButton = new JButton();
        addButton.setText("添加");
        panel3.add(addButton, new GridConstraints(3, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        contentPane.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        accountsTable = new JTable();
        accountsTable.setRowHeight(30);
        scrollPane1.setViewportView(accountsTable);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
