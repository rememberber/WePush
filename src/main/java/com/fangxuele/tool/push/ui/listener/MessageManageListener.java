package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.*;
import com.fangxuele.tool.push.ui.form.*;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.UiThreadUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * <pre>
 * 编辑消息tab相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/18.
 */
public class MessageManageListener {
    private static final Log logger = LogFactory.get();

    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    public static void addListeners() {
        JTable msgHistable = MessageManageForm.getInstance().getMsgHistable();
        JSplitPane messagePanel = MainWindow.getInstance().getMessagePanel();

        // 点击左侧表格事件
        msgHistable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int selectedRow = msgHistable.getSelectedRow();
                Integer selectedMsgId = (Integer) msgHistable.getValueAt(selectedRow, 1);

                MessageEditForm.init(selectedMsgId);
                super.mousePressed(e);
            }
        });

        // 历史消息管理-删除
        MessageManageForm.getInstance().getMsgHisTableDeleteButton().addActionListener(e -> {
            int[] selectedRows = msgHistable.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(messagePanel, "请至少选择一个！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int isDelete = JOptionPane.showConfirmDialog(messagePanel, "确认删除？", "确认",
                    JOptionPane.YES_NO_OPTION);
            if (isDelete != JOptionPane.YES_OPTION) {
                return;
            }
            DefaultTableModel tableModel = (DefaultTableModel) msgHistable.getModel();
            int[] rowsToDelete = selectedRows.clone();
            Integer[] msgIds = new Integer[rowsToDelete.length];
            for (int i = 0; i < rowsToDelete.length; i++) {
                msgIds[i] = (Integer) tableModel.getValueAt(rowsToDelete[i], 1);
            }
            ThreadUtil.execute(() -> {
                try {
                    for (Integer msgId : msgIds) {
                        msgMapper.deleteByPrimaryKey(msgId);
                    }
                    UiThreadUtil.runOnUi(() -> {
                        for (int i = rowsToDelete.length - 1; i >= 0; i--) {
                            tableModel.removeRow(rowsToDelete[i]);
                        }
                    });
                } catch (Exception e1) {
                    UiThreadUtil.runOnUi(() -> JOptionPane.showMessageDialog(messagePanel, "删除失败！\n\n" + e1.getMessage(), "失败",
                            JOptionPane.ERROR_MESSAGE));
                    logger.error(e1);
                }
            });
        });

        // 编辑消息-新建
        MessageManageForm.getInstance().getCreateMsgButton().addActionListener(e -> {
            MessageTypeForm.init();
            MessageEditForm.getInstance().getMsgNameField().setText("");
            MessageEditForm.getInstance().getMsgNameField().grabFocus();
        });

        // 切换账号事件
        MessageManageForm.getInstance().getAccountComboBox().addItemListener(e -> {
            if (MessageManageForm.accountSwitchComboBoxListenIgnore) {
                return;
            }
            if (e.getStateChange() == ItemEvent.SELECTED) {
                MessageManageForm.initMessageList();
            }
        });
    }
}