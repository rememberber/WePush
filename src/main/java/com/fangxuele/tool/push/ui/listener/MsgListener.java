package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.dao.TMsgKefuMapper;
import com.fangxuele.tool.push.dao.TMsgKefuPriorityMapper;
import com.fangxuele.tool.push.dao.TMsgMaTemplateMapper;
import com.fangxuele.tool.push.dao.TMsgMpTemplateMapper;
import com.fangxuele.tool.push.dao.TMsgSmsMapper;
import com.fangxuele.tool.push.logic.MsgHisManage;
import com.fangxuele.tool.push.logic.PushManage;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.MessageManageForm;
import com.fangxuele.tool.push.ui.form.PushHisForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <pre>
 * 编辑消息tab相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/18.
 */
public class MsgListener {
    private static final Log logger = LogFactory.get();

    public static MsgHisManage msgHisManager = MsgHisManage.getInstance();

    private TMsgKefuMapper msgKefuMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuMapper.class);
    private TMsgKefuPriorityMapper msgKefuPriorityMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuPriorityMapper.class);
    private TMsgMaTemplateMapper msgMaTemplateMapper = MybatisUtil.getSqlSession().getMapper(TMsgMaTemplateMapper.class);
    private TMsgMpTemplateMapper msgMpTemplateMapper = MybatisUtil.getSqlSession().getMapper(TMsgMpTemplateMapper.class);
    private TMsgSmsMapper msgSmsMapper = MybatisUtil.getSqlSession().getMapper(TMsgSmsMapper.class);

    public static void addListeners() {

        // 点击左侧表格事件
        MessageManageForm.messageManageForm.getMsgHistable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ThreadUtil.execute(() -> {
                    PushHisForm.pushHisForm.getPushHisTextArea().setText("");

                    int selectedRow = MessageManageForm.messageManageForm.getMsgHistable().getSelectedRow();
                    String selectedMsgName = MessageManageForm.messageManageForm.getMsgHistable()
                            .getValueAt(selectedRow, 1).toString();

                    MessageEditForm.init(selectedMsgName);
                });
                super.mouseClicked(e);
            }
        });

        // 客服消息类型切换事件
        MessageEditForm.messageEditForm.getMsgKefuMsgTypeComboBox().addItemListener(e -> MessageEditForm.switchKefuMsgType(e.getItem().toString()));

        // 模板数据-添加 按钮事件
        MessageEditForm.messageEditForm.getTemplateMsgDataAddButton().addActionListener(e -> {
            String[] data = new String[3];
            data[0] = MessageEditForm.messageEditForm.getTemplateDataNameTextField().getText();
            data[1] = MessageEditForm.messageEditForm.getTemplateDataValueTextField().getText();
            data[2] = MessageEditForm.messageEditForm.getTemplateDataColorTextField().getText();

            if (MessageEditForm.messageEditForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
                MessageEditForm.initTemplateDataTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) MessageEditForm.messageEditForm.getTemplateMsgDataTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();

            Set<String> keySet = new HashSet<>();
            String keyData;
            for (int i = 0; i < rowCount; i++) {
                keyData = (String) tableModel.getValueAt(i, 0);
                keySet.add(keyData);
            }

            if (StringUtils.isEmpty(data[0]) || StringUtils.isEmpty(data[1])) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "key或value不能为空！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (keySet.contains(data[0])) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "key不能重复！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                if (StringUtils.isEmpty(data[2])) {
                    data[2] = "#000000";
                } else if (!data[2].startsWith("#")) {
                    data[2] = "#" + data[2];
                }
                tableModel.addRow(data);
            }
        });

        // 保存按钮事件
        MessageEditForm.messageEditForm.getMsgSaveButton().addActionListener(e -> {
            String msgName = MessageEditForm.messageEditForm.getMsgNameField().getText();
            if (StringUtils.isBlank(msgName)) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "请填写推送任务名称！\n\n", "失败",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            Map<String, String[]> msgMap = msgHisManager.readMsgHis();

            int isCover = JOptionPane.NO_OPTION;
            if (msgMap.containsKey(msgName)) {
                // 如果存在，是否覆盖
                isCover = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getMessagePanel(), "已经存在同名的历史消息，\n是否覆盖？", "确认",
                        JOptionPane.YES_NO_OPTION);
            }

            try {
                if (!msgMap.containsKey(msgName) || isCover == JOptionPane.YES_OPTION) {
                    String[] record = new String[MsgHisManage.ARRAY_LENGTH];
                    record[0] = msgName;
                    record[1] = String.valueOf(Init.config.getMsgType());
                    record[2] = MessageEditForm.messageEditForm.getMsgTemplateIdTextField().getText();
                    record[3] = MessageEditForm.messageEditForm.getMsgTemplateUrlTextField().getText();
                    record[4] = Objects.requireNonNull(MessageEditForm.messageEditForm.getMsgKefuMsgTypeComboBox().getSelectedItem()).toString();
                    record[5] = MessageEditForm.messageEditForm.getMsgKefuMsgTitleTextField().getText();
                    record[6] = MessageEditForm.messageEditForm.getMsgKefuPicUrlTextField().getText();
                    record[7] = MessageEditForm.messageEditForm.getMsgKefuDescTextField().getText();
                    record[8] = MessageEditForm.messageEditForm.getMsgKefuUrlTextField().getText();
                    record[9] = MessageEditForm.messageEditForm.getMsgTemplateMiniAppidTextField().getText();
                    record[10] = MessageEditForm.messageEditForm.getMsgTemplateMiniPagePathTextField().getText();
                    record[11] = MessageEditForm.messageEditForm.getMsgTemplateKeyWordTextField().getText();
                    record[12] = MessageEditForm.messageEditForm.getMsgYunpianMsgContentTextField().getText();

                    msgMap.put(msgName, record);

                    msgHisManager.writeMsgHis(msgMap);
                    msgHisManager.writeTemplateData(msgName);

                    Init.config.setMsgName(msgName);
                    Init.config.setPreviewUser(MessageEditForm.messageEditForm.getPreviewUserField().getText());
                    Init.config.save();

                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存成功！", "成功",
                            JOptionPane.INFORMATION_MESSAGE);

                    MessageEditForm.init(null);
                    MessageManageForm.init();
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }

        });

        // 预览按钮事件
        MessageEditForm.messageEditForm.getPreviewMsgButton().addActionListener(e -> {
            try {
                if ("".equals(MessageEditForm.messageEditForm.getPreviewUserField().getText().trim())) {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "预览消息用户不能为空！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    if (PushManage.preview()) {
                        JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "发送预览消息成功！", "成功",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "发送预览消息失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 编辑消息-新建
        MessageEditForm.messageEditForm.getCreateMsgButton().addActionListener(e -> {
            MessageEditForm.messageEditForm.getMsgNameField().setText("");
            MessageEditForm.messageEditForm.getMsgTemplateIdTextField().setText("");
            MessageEditForm.messageEditForm.getMsgTemplateUrlTextField().setText("");
            MessageEditForm.messageEditForm.getMsgKefuMsgTitleTextField().setText("");
            MessageEditForm.messageEditForm.getMsgKefuPicUrlTextField().setText("");
            MessageEditForm.messageEditForm.getMsgKefuDescTextField().setText("");
            MessageEditForm.messageEditForm.getMsgKefuUrlTextField().setText("");
            MessageEditForm.messageEditForm.getMsgTemplateMiniAppidTextField().setText("");
            MessageEditForm.messageEditForm.getMsgTemplateMiniPagePathTextField().setText("");
            MessageEditForm.messageEditForm.getMsgTemplateKeyWordTextField().setText("");
            MessageEditForm.messageEditForm.getMsgYunpianMsgContentTextField().setText("");

            if (MessageEditForm.messageEditForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
                MessageEditForm.initTemplateDataTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) MessageEditForm.messageEditForm.getTemplateMsgDataTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                tableModel.removeRow(0);
            }
        });
    }
}
