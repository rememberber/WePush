package com.fangxuele.tool.wechat.push.ui.listener;

import com.fangxuele.tool.wechat.push.ui.Init;
import com.fangxuele.tool.wechat.push.ui.MainWindow;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.LogFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;

/**
 * 推送历史管理tab相关事件监听
 * Created by rememberber(https://github.com/rememberber) on 2017/6/16.
 */
public class PushHisListener {
    private static final Log logger = LogFactory.get();

    public static void addListeners() {

        // 导入历史管理-全选
        MainWindow.mainWindow.getPushHisLeftSelectAllButton().addActionListener(e -> new Thread(() -> {
            DefaultTableModel tableModel = (DefaultTableModel) MainWindow.mainWindow.getPushHisLeftTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                tableModel.setValueAt(true, i, 0);
            }
        }).start());

        // 导入历史管理-全不选
        MainWindow.mainWindow.getPushHisLeftUnselectAllButton().addActionListener(e -> new Thread(() -> {
            DefaultTableModel tableModel = (DefaultTableModel) MainWindow.mainWindow.getPushHisLeftTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                tableModel.setValueAt(false, i, 0);
            }
        }).start());

        // 导入历史管理-删除
        MainWindow.mainWindow.getPushHisLeftDeleteButton().addActionListener(e -> new Thread(() -> {
            int isDelete = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getSettingPanel(), "确认删除？", "确认",
                    JOptionPane.INFORMATION_MESSAGE);
            if (isDelete == JOptionPane.YES_OPTION) {
                try {
                    DefaultTableModel tableModel = (DefaultTableModel) MainWindow.mainWindow.getPushHisLeftTable()
                            .getModel();
                    int rowCount = tableModel.getRowCount();
                    for (int i = 0; i < rowCount; ) {
                        boolean delete = (boolean) tableModel.getValueAt(i, 0);
                        if (delete) {
                            String fileName = (String) tableModel.getValueAt(i, 1);
                            File msgTemplateDataFile = new File("data/push_his/" + fileName);
                            if (msgTemplateDataFile.exists()) {
                                msgTemplateDataFile.delete();
                            }
                            tableModel.removeRow(i);
                            MainWindow.mainWindow.getImportHisTable().updateUI();
                            i = 0;
                            rowCount = tableModel.getRowCount();
                            continue;
                        } else {
                            i++;
                        }
                    }

                    Init.initMemberTab();
                } catch (Exception e1) {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "删除失败！\n\n" + e1.getMessage(), "失败",
                            JOptionPane.ERROR_MESSAGE);
                    logger.error(e1);
                }
            }
        }).start());

    }
}
