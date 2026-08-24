package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.logic.msgsender.MsgSenderFactory;
import com.fangxuele.tool.push.ui.form.*;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.UiThreadUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * <pre>
 * 编辑账号tab相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2021/3/18.
 */
public class AccountManageListener {
    private static final Log logger = LogFactory.get();

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);

    public static void addListeners() {
        JTable accountListTable = AccountManageForm.getInstance().getAccountListTable();
        JSplitPane accountPanel = MainWindow.getInstance().getAccountPanel();

        // 点击左侧表格事件
        accountListTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int selectedRow = accountListTable.getSelectedRow();
                String selectedMsgName = accountListTable
                        .getValueAt(selectedRow, 0).toString();

                AccountEditForm.init(selectedMsgName);
                super.mousePressed(e);
            }
        });

        // 账号管理-删除
        AccountManageForm.getInstance().getAccountListTableDeleteButton().addActionListener(e -> {
            int[] selectedRows = accountListTable.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(accountPanel, "请至少选择一个！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int isDelete = JOptionPane.showConfirmDialog(accountPanel, "确认删除？", "确认",
                    JOptionPane.YES_NO_OPTION);
            if (isDelete != JOptionPane.YES_OPTION) {
                return;
            }
            DefaultTableModel tableModel = (DefaultTableModel) accountListTable.getModel();
            int msgType = App.config.getMsgType();
            int[] rowsToDelete = selectedRows.clone();
            String[] accountNames = new String[rowsToDelete.length];
            for (int i = 0; i < rowsToDelete.length; i++) {
                accountNames[i] = (String) tableModel.getValueAt(rowsToDelete[i], 0);
            }
            ThreadUtil.execute(() -> {
                try {
                    for (String accountName : accountNames) {
                        TAccount account = accountMapper.selectByMsgTypeAndAccountName(msgType, accountName);
                        accountMapper.deleteByMsgTypeAndAccountName(msgType, accountName);
                        if (account != null) {
                            MsgSenderFactory.removeAccount(msgType, account.getId());
                        }
                    }
                    UiThreadUtil.runOnUi(() -> {
                        for (int i = rowsToDelete.length - 1; i >= 0; i--) {
                            tableModel.removeRow(rowsToDelete[i]);
                        }
                        AccountEditForm.init(null);
                        PeopleManageForm.init();
                    });
                } catch (Exception e1) {
                    UiThreadUtil.runOnUi(() -> JOptionPane.showMessageDialog(accountPanel, "删除失败！\n\n" + e1.getMessage(), "失败",
                            JOptionPane.ERROR_MESSAGE));
                    logger.error(e1);
                }
            });
        });

        // 编辑账号-新建
        AccountManageForm.getInstance().getCreateAccountButton().addActionListener(e -> {
//            MessageTypeForm.init();
            AccountEditForm.clear();

            AccountEditForm.getInstance().getAccountNameField().grabFocus();
        });
    }
}
