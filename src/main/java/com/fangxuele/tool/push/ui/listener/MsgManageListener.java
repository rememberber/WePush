package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TMsgKefuMapper;
import com.fangxuele.tool.push.dao.TMsgKefuPriorityMapper;
import com.fangxuele.tool.push.dao.TMsgMaTemplateMapper;
import com.fangxuele.tool.push.dao.TMsgMpTemplateMapper;
import com.fangxuele.tool.push.dao.TMsgSmsMapper;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.MessageManageForm;
import com.fangxuele.tool.push.ui.form.MessageTypeForm;
import com.fangxuele.tool.push.ui.form.PushHisForm;
import com.fangxuele.tool.push.util.MybatisUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
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
public class MsgManageListener {
    private static final Log logger = LogFactory.get();

    private static TMsgKefuMapper msgKefuMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuMapper.class);
    private static TMsgKefuPriorityMapper msgKefuPriorityMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuPriorityMapper.class);
    private static TMsgMaTemplateMapper msgMaTemplateMapper = MybatisUtil.getSqlSession().getMapper(TMsgMaTemplateMapper.class);
    private static TMsgMpTemplateMapper msgMpTemplateMapper = MybatisUtil.getSqlSession().getMapper(TMsgMpTemplateMapper.class);
    private static TMsgSmsMapper msgSmsMapper = MybatisUtil.getSqlSession().getMapper(TMsgSmsMapper.class);

    private static JTable msgHistable = MessageManageForm.messageManageForm.getMsgHistable();
    private static JSplitPane messagePanel = MainWindow.mainWindow.getMessagePanel();

    public static void addListeners() {

        // 点击左侧表格事件
        msgHistable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                ThreadUtil.execute(() -> {
                    PushHisForm.pushHisForm.getPushHisTextArea().setText("");

                    int selectedRow = msgHistable.getSelectedRow();
                    String selectedMsgName = msgHistable
                            .getValueAt(selectedRow, 0).toString();

                    MessageEditForm.init(selectedMsgName);
                });
                super.mousePressed(e);
            }
        });

        // 历史消息管理-删除
        MessageManageForm.messageManageForm.getMsgHisTableDeleteButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                int[] selectedRows = msgHistable.getSelectedRows();

                if (selectedRows.length == 0) {
                    JOptionPane.showMessageDialog(messagePanel, "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    int isDelete = JOptionPane.showConfirmDialog(messagePanel, "确认删除？", "确认",
                            JOptionPane.YES_NO_OPTION);
                    if (isDelete == JOptionPane.YES_OPTION) {
                        DefaultTableModel tableModel = (DefaultTableModel) msgHistable
                                .getModel();
                        int msgType = App.config.getMsgType();

                        for (int i = selectedRows.length; i > 0; i--) {
                            int selectedRow = msgHistable.getSelectedRow();
                            String msgName = (String) tableModel.getValueAt(selectedRow, 0);
                            if (msgType == MessageTypeEnum.KEFU_CODE) {
                                msgKefuMapper.deleteByMsgTypeAndName(msgType, msgName);
                            } else if (msgType == MessageTypeEnum.KEFU_PRIORITY_CODE) {
                                msgKefuPriorityMapper.deleteByMsgTypeAndName(msgType, msgName);
                            } else if (msgType == MessageTypeEnum.MA_TEMPLATE_CODE) {
                                msgMaTemplateMapper.deleteByMsgTypeAndName(msgType, msgName);
                            } else if (msgType == MessageTypeEnum.MP_TEMPLATE_CODE) {
                                msgMpTemplateMapper.deleteByMsgTypeAndName(msgType, msgName);
                            } else {
                                msgSmsMapper.deleteByMsgTypeAndName(msgType, msgName);
                            }

                            tableModel.removeRow(selectedRow);
                        }
                        MessageEditForm.init(null);
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(messagePanel, "删除失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        }));

        // 编辑消息-新建
        MessageManageForm.messageManageForm.getCreateMsgButton().addActionListener(e -> {
            MessageTypeForm.init();
            MessageEditForm.messageEditForm.getMsgNameField().setText("");
            MessageEditForm.messageEditForm.getMsgNameField().grabFocus();
        });
    }
}