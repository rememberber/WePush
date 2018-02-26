package com.fangxuele.tool.wechat.push.ui.listener;

import com.fangxuele.tool.wechat.push.ui.Init;
import com.fangxuele.tool.wechat.push.ui.MainWindow;
import com.fangxuele.tool.wechat.push.util.SystemUtil;
import com.xiaoleilu.hutool.io.FileUtil;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.LogFactory;
import com.xiaoleilu.hutool.util.ClipboardUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * 推送历史管理tab相关事件监听
 * Created by rememberber(https://github.com/rememberber) on 2017/6/16.
 */
public class PushHisListener {
    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        // 点击左侧表格事件
        MainWindow.mainWindow.getPushHisLeftTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MainWindow.mainWindow.getPushHisTextArea().setText("");

                        int selectedRow = MainWindow.mainWindow.getPushHisLeftTable().getSelectedRow();
                        String selectedFileName = MainWindow.mainWindow.getPushHisLeftTable()
                                .getValueAt(selectedRow, 1).toString();
                        File pushHisFile = new File(SystemUtil.configHome + "data" + File.separator
                                + "push_his" + File.separator + selectedFileName);

                        try {
                            BufferedReader br = new BufferedReader(new FileReader(pushHisFile));
                            String line = br.readLine();
                            long count = 0;
                            while (StringUtils.isNotEmpty(line)) {
                                MainWindow.mainWindow.getPushHisTextArea().append(line);
                                MainWindow.mainWindow.getPushHisTextArea().append("\n");
                                line = br.readLine();
                                count++;
                            }

                            MainWindow.mainWindow.getPushHisCountLabel().setText("共" + count + "条");
                        } catch (FileNotFoundException e1) {
                            e1.printStackTrace();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }

                    }
                }).start();
                super.mouseClicked(e);
            }
        });

        // 推送历史管理-全选
        MainWindow.mainWindow.getPushHisLeftSelectAllButton().addActionListener(e -> new Thread(() -> {
            DefaultTableModel tableModel = (DefaultTableModel) MainWindow.mainWindow.getPushHisLeftTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                tableModel.setValueAt(true, i, 0);
            }
        }).start());

        // 推送历史管理-全不选
        MainWindow.mainWindow.getPushHisLeftUnselectAllButton().addActionListener(e -> new Thread(() -> {
            DefaultTableModel tableModel = (DefaultTableModel) MainWindow.mainWindow.getPushHisLeftTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                tableModel.setValueAt(false, i, 0);
            }
        }).start());

        // 推送历史管理-删除
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
                            File msgTemplateDataFile = new File(SystemUtil.configHome + "data" + File.separator + "push_his" + File.separator + fileName);
                            if (msgTemplateDataFile.exists()) {
                                msgTemplateDataFile.delete();
                            }
                            tableModel.removeRow(i);
                            MainWindow.mainWindow.getPushHisLeftTable().updateUI();
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

        // 推送历史管理-复制按钮
        MainWindow.mainWindow.getPushHisCopyButton().addActionListener(e -> new Thread(() -> {
            try {
                MainWindow.mainWindow.getPushHisCopyButton().setEnabled(false);
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "内容已经复制到剪贴板！", "复制成功",
                        JOptionPane.INFORMATION_MESSAGE);
                ClipboardUtil.setStr(MainWindow.mainWindow.getPushHisTextArea().getText());
            } catch (Exception e1) {
                logger.error(e1);
            } finally {
                MainWindow.mainWindow.getPushHisCopyButton().setEnabled(true);
            }

        }).start());

        // 推送历史管理-导出按钮
        MainWindow.mainWindow.getPushHisExportButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int approve = fileChooser.showOpenDialog(MainWindow.mainWindow.getSettingPanel());
                if (approve == JFileChooser.APPROVE_OPTION) {
                    String exportPath = fileChooser.getSelectedFile().getAbsolutePath();

                    try {
                        DefaultTableModel tableModel = (DefaultTableModel) MainWindow.mainWindow.getPushHisLeftTable()
                                .getModel();
                        int rowCount = tableModel.getRowCount();
                        for (int i = 0; i < rowCount; ) {
                            boolean delete = (boolean) tableModel.getValueAt(i, 0);
                            if (delete) {
                                String fileName = (String) tableModel.getValueAt(i, 1);
                                File msgTemplateDataFile = new File(SystemUtil.configHome + "data" + File.separator + "push_his" + File.separator + fileName);
                                if (msgTemplateDataFile.exists()) {
                                    FileUtil.copy(msgTemplateDataFile.getAbsolutePath(), exportPath, true);
                                }
                                tableModel.removeRow(i);
                                MainWindow.mainWindow.getPushHisLeftTable().updateUI();
                                i = 0;
                                rowCount = tableModel.getRowCount();
                                continue;
                            } else {
                                i++;
                            }
                        }

                        JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "导出成功！", "提示",
                                JOptionPane.INFORMATION_MESSAGE);
                        try {
                            Desktop desktop = Desktop.getDesktop();
                            desktop.open(new File(exportPath));
                        } catch (Exception e2) {
                            logger.error(e2);
                        }

                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "导出失败！\n\n" + e1.getMessage(), "失败",
                                JOptionPane.ERROR_MESSAGE);
                        logger.error(e1);
                    }
                }
            }
        });

    }
}
