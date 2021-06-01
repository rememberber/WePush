package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.*;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.form.*;
import com.fangxuele.tool.push.util.MybatisUtil;

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

    private static TMsgKefuMapper msgKefuMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuMapper.class);
    private static TMsgKefuPriorityMapper msgKefuPriorityMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuPriorityMapper.class);
    private static TMsgWxUniformMapper wxUniformMapper = MybatisUtil.getSqlSession().getMapper(TMsgWxUniformMapper.class);
    private static TMsgMaTemplateMapper msgMaTemplateMapper = MybatisUtil.getSqlSession().getMapper(TMsgMaTemplateMapper.class);
    private static TMsgMaSubscribeMapper msgMaSubscribeMapper = MybatisUtil.getSqlSession().getMapper(TMsgMaSubscribeMapper.class);
    private static TMsgMpTemplateMapper msgMpTemplateMapper = MybatisUtil.getSqlSession().getMapper(TMsgMpTemplateMapper.class);
    private static TMsgMpSubscribeMapper msgMpSubscribeMapper = MybatisUtil.getSqlSession().getMapper(TMsgMpSubscribeMapper.class);
    private static TMsgSmsMapper msgSmsMapper = MybatisUtil.getSqlSession().getMapper(TMsgSmsMapper.class);
    private static TMsgMailMapper msgMailMapper = MybatisUtil.getSqlSession().getMapper(TMsgMailMapper.class);
    private static TMsgHttpMapper msgHttpMapper = MybatisUtil.getSqlSession().getMapper(TMsgHttpMapper.class);
    private static TMsgWxCpMapper msgWxCpMapper = MybatisUtil.getSqlSession().getMapper(TMsgWxCpMapper.class);
    private static TMsgDingMapper msgDingMapper = MybatisUtil.getSqlSession().getMapper(TMsgDingMapper.class);
    private static TWxAccountMapper wxAccountMapper = MybatisUtil.getSqlSession().getMapper(TWxAccountMapper.class);

    public static void addListeners() {
        JTable msgHistable = MessageManageForm.getInstance().getMsgHistable();
        JSplitPane messagePanel = MainWindow.getInstance().getMessagePanel();

        // 点击左侧表格事件
        msgHistable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                PushHisForm.getInstance().getPushHisTextArea().setText("");

                int selectedRow = msgHistable.getSelectedRow();
                Integer selectedMsgId = (Integer) msgHistable.getValueAt(selectedRow, 1);

                MessageEditForm.init(selectedMsgId);
                super.mousePressed(e);
            }
        });

        // 历史消息管理-删除
        MessageManageForm.getInstance().getMsgHisTableDeleteButton().addActionListener(e -> ThreadUtil.execute(() -> {
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
                            Integer msgId = (Integer) tableModel.getValueAt(selectedRow, 1);
                            if (msgType == MessageTypeEnum.KEFU_CODE) {
                                msgKefuMapper.deleteByPrimaryKey(msgId);
                            } else if (msgType == MessageTypeEnum.KEFU_PRIORITY_CODE) {
                                msgKefuPriorityMapper.deleteByPrimaryKey(msgId);
                            } else if (msgType == MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE) {
                                wxUniformMapper.deleteByPrimaryKey(msgId);
                            } else if (msgType == MessageTypeEnum.MA_TEMPLATE_CODE) {
                                msgMaTemplateMapper.deleteByPrimaryKey(msgId);
                            } else if (msgType == MessageTypeEnum.MA_SUBSCRIBE_CODE) {
                                msgMaSubscribeMapper.deleteByPrimaryKey(msgId);
                            } else if (msgType == MessageTypeEnum.MP_TEMPLATE_CODE) {
                                msgMpTemplateMapper.deleteByPrimaryKey(msgId);
                            } else if (msgType == MessageTypeEnum.MP_SUBSCRIBE_CODE) {
                                msgMpSubscribeMapper.deleteByPrimaryKey(msgId);
                            } else if (msgType == MessageTypeEnum.EMAIL_CODE) {
                                msgMailMapper.deleteByPrimaryKey(msgId);
                            } else if (msgType == MessageTypeEnum.HTTP_CODE) {
                                msgHttpMapper.deleteByPrimaryKey(msgId);
                            } else if (msgType == MessageTypeEnum.WX_CP_CODE) {
                                msgWxCpMapper.deleteByPrimaryKey(msgId);
                            } else if (msgType == MessageTypeEnum.DING_CODE) {
                                msgDingMapper.deleteByPrimaryKey(msgId);
                            } else {
                                msgSmsMapper.deleteByPrimaryKey(msgId);
                            }

                            tableModel.removeRow(selectedRow);
                        }
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(messagePanel, "删除失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        }));

        // 编辑消息-新建
        MessageManageForm.getInstance().getCreateMsgButton().addActionListener(e -> {
            MessageTypeForm.init();
            MessageEditForm.getInstance().getMsgNameField().setText("");
            MessageEditForm.getInstance().getMsgNameField().grabFocus();
        });

        // 切换账号事件
        MessageManageForm.getInstance().getAccountComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String accountName = e.getItem().toString();

                MessageManageForm.initMessageList();
            }
        });
    }
}