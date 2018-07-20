package com.fangxuele.tool.push.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.logic.MsgHisManage;
import com.fangxuele.tool.push.logic.PushManage;
import com.fangxuele.tool.push.ui.MainWindow;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 编辑消息tab相关事件监听
 * Created by rememberber(https://github.com/rememberber) on 2017/6/18.
 */
public class MsgListener {
    private static final Log logger = LogFactory.get();

    public static MsgHisManage msgHisManager = MsgHisManage.getInstance();

    public static void addListeners() {

        // 点击左侧表格事件
        MainWindow.mainWindow.getMsgHistable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new Thread(() -> {
                    MainWindow.mainWindow.getPushHisTextArea().setText("");

                    int selectedRow = MainWindow.mainWindow.getMsgHistable().getSelectedRow();
                    String selectedMsgName = MainWindow.mainWindow.getMsgHistable()
                            .getValueAt(selectedRow, 1).toString();

                    Init.initMsgTab(selectedMsgName);
                }).start();
                super.mouseClicked(e);
            }
        });

        // 消息类型切换事件
        MainWindow.mainWindow.getMsgTypeComboBox().addItemListener(e -> Init.switchMsgType(e.getItem().toString()));

        // 客服消息类型切换事件
        MainWindow.mainWindow.getMsgKefuMsgTypeComboBox().addItemListener(e -> Init.switchKefuMsgType(e.getItem().toString()));

        // 模板数据-添加 按钮事件
        MainWindow.mainWindow.getTemplateMsgDataAddButton().addActionListener(e -> {
            String[] data = new String[3];
            data[0] = MainWindow.mainWindow.getTemplateDataNameTextField().getText();
            data[1] = MainWindow.mainWindow.getTemplateDataValueTextField().getText();
            data[2] = MainWindow.mainWindow.getTemplateDataColorTextField().getText();

            if (MainWindow.mainWindow.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
                Init.initTemplateDataTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) MainWindow.mainWindow.getTemplateMsgDataTable()
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
        MainWindow.mainWindow.getMsgSaveButton().addActionListener(e -> {
            String msgName = MainWindow.mainWindow.getMsgNameField().getText();
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
                        JOptionPane.INFORMATION_MESSAGE);
            }

            try {
                if (!msgMap.containsKey(msgName) || isCover == JOptionPane.YES_OPTION) {
                    String[] record = new String[MsgHisManage.ARRAY_LENGTH];
                    record[0] = msgName;
                    record[1] = MainWindow.mainWindow.getMsgTypeComboBox().getSelectedItem().toString();
                    record[2] = MainWindow.mainWindow.getMsgTemplateIdTextField().getText();
                    record[3] = MainWindow.mainWindow.getMsgTemplateUrlTextField().getText();
                    record[4] = MainWindow.mainWindow.getMsgKefuMsgTypeComboBox().getSelectedItem().toString();
                    record[5] = MainWindow.mainWindow.getMsgKefuMsgTitleTextField().getText();
                    record[6] = MainWindow.mainWindow.getMsgKefuPicUrlTextField().getText();
                    record[7] = MainWindow.mainWindow.getMsgKefuDescTextField().getText();
                    record[8] = MainWindow.mainWindow.getMsgKefuUrlTextField().getText();
                    record[9] = MainWindow.mainWindow.getMsgTemplateMiniAppidTextField().getText();
                    record[10] = MainWindow.mainWindow.getMsgTemplateMiniPagePathTextField().getText();
                    record[11] = MainWindow.mainWindow.getMsgTemplateKeyWordTextField().getText();
                    record[12] = MainWindow.mainWindow.getMsgYunpianMsgContentTextField().getText();

                    msgMap.put(msgName, record);

                    msgHisManager.writeMsgHis(msgMap);
                    msgHisManager.writeTemplateData(msgName);

                    Init.configer.setMsgName(msgName);
                    Init.configer.setPreviewUser(MainWindow.mainWindow.getPreviewUserField().getText());
                    Init.configer.save();

                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存成功！", "成功",
                            JOptionPane.INFORMATION_MESSAGE);

                    Init.initMsgTab(null);
                    Init.initSettingTab();
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }

        });

        // 预览按钮事件
        MainWindow.mainWindow.getPreviewMsgButton().addActionListener(e -> {
            try {
                if ("".equals(MainWindow.mainWindow.getPreviewUserField().getText().trim())) {
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
        MainWindow.mainWindow.getCreateMsgButton().addActionListener(e -> {
            MainWindow.mainWindow.getMsgNameField().setText("");
            MainWindow.mainWindow.getMsgTemplateIdTextField().setText("");
            MainWindow.mainWindow.getMsgTemplateUrlTextField().setText("");
            MainWindow.mainWindow.getMsgKefuMsgTitleTextField().setText("");
            MainWindow.mainWindow.getMsgKefuPicUrlTextField().setText("");
            MainWindow.mainWindow.getMsgKefuDescTextField().setText("");
            MainWindow.mainWindow.getMsgKefuUrlTextField().setText("");
            MainWindow.mainWindow.setMsgTemplateMiniAppidTextField("");
            MainWindow.mainWindow.setMsgTemplateMiniPagePathTextField("");
            MainWindow.mainWindow.setMsgTemplateKeyWordTextField("");
            MainWindow.mainWindow.getMsgYunpianMsgContentTextField().setText("");

            if (MainWindow.mainWindow.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
                Init.initTemplateDataTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) MainWindow.mainWindow.getTemplateMsgDataTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                tableModel.removeRow(0);
            }
        });
    }
}
