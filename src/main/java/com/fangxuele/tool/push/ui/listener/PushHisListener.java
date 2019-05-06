package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.swing.ClipboardUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.PushHisForm;
import com.fangxuele.tool.push.util.SystemUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * 推送历史管理tab相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/16.
 */
public class PushHisListener {
    private static final Log logger = LogFactory.get();

    private static boolean selectAllToggle = false;

    public static void addListeners() {
        // 点击左侧表格事件
        PushHisForm.pushHisForm.getPushHisLeftTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ThreadUtil.execute(() -> {
                    PushHisForm.pushHisForm.getPushHisTextArea().setText("");

                    int selectedRow = PushHisForm.pushHisForm.getPushHisLeftTable().getSelectedRow();
                    String selectedFileName = PushHisForm.pushHisForm.getPushHisLeftTable()
                            .getValueAt(selectedRow, 1).toString();
                    File pushHisFile = new File(SystemUtil.configHome + "data" + File.separator
                            + "push_his" + File.separator + selectedFileName);

                    try {
                        BufferedReader br = new BufferedReader(new FileReader(pushHisFile));
                        String line = br.readLine();
                        long count = 0;
                        while (StringUtils.isNotEmpty(line)) {
                            PushHisForm.pushHisForm.getPushHisTextArea().append(line);
                            PushHisForm.pushHisForm.getPushHisTextArea().append("\n");
                            line = br.readLine();
                            count++;
                        }

                        PushHisForm.pushHisForm.getPushHisCountLabel().setText("共" + count + "条");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                });
                super.mouseClicked(e);
            }
        });

        // 推送历史管理-全选
        PushHisForm.pushHisForm.getPushHisLeftSelectAllButton().addActionListener(e -> ThreadUtil.execute(() -> {
            toggleSelectAll();
            DefaultTableModel tableModel = (DefaultTableModel) PushHisForm.pushHisForm.getPushHisLeftTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                tableModel.setValueAt(selectAllToggle, i, 0);
            }
        }));

        // 推送历史管理-删除
        PushHisForm.pushHisForm.getPushHisLeftDeleteButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                DefaultTableModel tableModel = (DefaultTableModel) PushHisForm.pushHisForm.getPushHisLeftTable()
                        .getModel();
                int rowCount = tableModel.getRowCount();

                int selectedCount = 0;
                for (int i = 0; i < rowCount; i++) {
                    boolean isSelected = (boolean) tableModel.getValueAt(i, 0);
                    if (isSelected) {
                        selectedCount++;
                    }
                }

                if (selectedCount == 0) {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    int isDelete = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getSettingPanel(), "确认删除？", "确认",
                            JOptionPane.YES_NO_OPTION);
                    if (isDelete == JOptionPane.YES_OPTION) {
                        for (int i = 0; i < rowCount; ) {
                            boolean delete = (boolean) tableModel.getValueAt(i, 0);
                            if (delete) {
                                String fileName = (String) tableModel.getValueAt(i, 1);
                                File msgTemplateDataFile = new File(SystemUtil.configHome + "data" + File.separator + "push_his" + File.separator + fileName);
                                if (msgTemplateDataFile.exists()) {
                                    msgTemplateDataFile.delete();
                                }
                                tableModel.removeRow(i);
                                PushHisForm.pushHisForm.getPushHisLeftTable().updateUI();
                                i = 0;
                                rowCount = tableModel.getRowCount();
                            } else {
                                i++;
                            }
                        }

                        Init.initMemberTab();
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "删除失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        }));

        // 推送历史管理-复制按钮
        PushHisForm.pushHisForm.getPushHisCopyButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                PushHisForm.pushHisForm.getPushHisCopyButton().setEnabled(false);
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "内容已经复制到剪贴板！", "复制成功",
                        JOptionPane.INFORMATION_MESSAGE);
                ClipboardUtil.setStr(PushHisForm.pushHisForm.getPushHisTextArea().getText());
            } catch (Exception e1) {
                logger.error(e1);
            } finally {
                PushHisForm.pushHisForm.getPushHisCopyButton().setEnabled(true);
            }

        }));

        // 推送历史管理-导出按钮
        PushHisForm.pushHisForm.getPushHisExportButton().addActionListener(e -> {
            List<String> toExportFilePathList = new ArrayList<>();
            int selectedCount = 0;

            try {
                DefaultTableModel tableModel = (DefaultTableModel) PushHisForm.pushHisForm.getPushHisLeftTable()
                        .getModel();
                int rowCount = tableModel.getRowCount();
                for (int i = 0; i < rowCount; i++) {
                    boolean selected = (boolean) tableModel.getValueAt(i, 0);
                    if (selected) {
                        selectedCount++;
                        String fileName = (String) tableModel.getValueAt(i, 1);
                        File msgTemplateDataFile = new File(SystemUtil.configHome + "data" + File.separator + "push_his" + File.separator + fileName);
                        if (msgTemplateDataFile.exists()) {
                            toExportFilePathList.add(msgTemplateDataFile.getAbsolutePath());
                        }
                    }
                }

                if (selectedCount > 0) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int approve = fileChooser.showOpenDialog(MainWindow.mainWindow.getSettingPanel());
                    String exportPath = "";
                    if (approve == JFileChooser.APPROVE_OPTION) {
                        exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                    } else {
                        return;
                    }

                    for (String toExportFilePath : toExportFilePathList) {
                        FileUtil.copy(toExportFilePath, exportPath, true);
                    }
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "导出成功！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(new File(exportPath));
                    } catch (Exception e2) {
                        logger.error(e2);
                    }
                } else {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }

            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "导出失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }

        });

    }

    /**
     * 切换全选/全不选
     *
     * @return
     */
    private static void toggleSelectAll() {
        if (!selectAllToggle) {
            selectAllToggle = true;
        } else {
            selectAllToggle = false;
        }
    }
}
