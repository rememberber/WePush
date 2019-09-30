package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.swing.clipboard.ClipboardUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.dao.TPushHistoryMapper;
import com.fangxuele.tool.push.domain.TPushHistory;
import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MemberForm;
import com.fangxuele.tool.push.ui.form.PushHisForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

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

    private static TPushHistoryMapper pushHistoryMapper = MybatisUtil.getSqlSession().getMapper(TPushHistoryMapper.class);

    public static void addListeners() {
        JTable pushHisLeftTable = PushHisForm.getInstance().getPushHisLeftTable();
        JPanel pushHisPanel = MainWindow.getInstance().getPushHisPanel();
        PushHisForm pushHisForm = PushHisForm.getInstance();
        MemberForm memberForm = MemberForm.getInstance();

        // 点击左侧表格事件
        pushHisLeftTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                ThreadUtil.execute(() -> {
                    pushHisForm.getPushHisTextArea().setText("");

                    int selectedRow = pushHisLeftTable.getSelectedRow();
                    String selectedId = pushHisLeftTable
                            .getValueAt(selectedRow, 3).toString();
                    TPushHistory tPushHistory = pushHistoryMapper.selectByPrimaryKey(Integer.valueOf(selectedId));
                    File pushHisFile = new File(tPushHistory.getCsvFile());

                    try {
                        BufferedReader br = new BufferedReader(new FileReader(pushHisFile));
                        String line = br.readLine();
                        long count = 0;
                        pushHisForm.getProgressBar().setVisible(true);
                        pushHisForm.getProgressBar().setIndeterminate(true);
                        pushHisForm.getContentScrollPane().setVisible(false);
                        pushHisForm.getContentControllPanel().setVisible(false);
                        while (StringUtils.isNotEmpty(line)) {
                            pushHisForm.getPushHisTextArea().append(line);
                            pushHisForm.getPushHisTextArea().append("\n");
                            line = br.readLine();
                            count++;
                        }
                        pushHisForm.getContentScrollPane().setVisible(true);
                        pushHisForm.getContentControllPanel().setVisible(true);
                        pushHisForm.getProgressBar().setVisible(false);

                        pushHisForm.getPushHisCountLabel().setText("共" + count + "条");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                });
                super.mousePressed(e);
            }
        });

        // 推送历史管理-删除
        pushHisForm.getPushHisLeftDeleteButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                int[] selectedRows = pushHisLeftTable.getSelectedRows();
                if (selectedRows.length == 0) {
                    JOptionPane.showMessageDialog(pushHisPanel, "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    int isDelete = JOptionPane.showConfirmDialog(pushHisPanel, "确认删除？", "确认",
                            JOptionPane.YES_NO_OPTION);
                    if (isDelete == JOptionPane.YES_OPTION) {
                        DefaultTableModel tableModel = (DefaultTableModel) pushHisLeftTable.getModel();
                        for (int i = selectedRows.length; i > 0; i--) {
                            int selectedRow = pushHisLeftTable.getSelectedRow();
                            Integer selectedId = (Integer) tableModel.getValueAt(selectedRow, 3);
                            TPushHistory tPushHistory = pushHistoryMapper.selectByPrimaryKey(selectedId);

                            File msgTemplateDataFile = new File(tPushHistory.getCsvFile());
                            if (msgTemplateDataFile.exists()) {
                                msgTemplateDataFile.delete();
                            }
                            pushHistoryMapper.deleteByPrimaryKey(selectedId);

                            tableModel.removeRow(selectedRow);
                        }
                        pushHisLeftTable.updateUI();
                        PushHisForm.init();
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(pushHisPanel, "删除失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        }));

        // 推送历史管理-复制按钮
        pushHisForm.getPushHisCopyButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                pushHisForm.getPushHisCopyButton().setEnabled(false);
                ClipboardUtil.setStr(pushHisForm.getPushHisTextArea().getText());
                JOptionPane.showMessageDialog(pushHisPanel, "内容已经复制到剪贴板！", "复制成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                logger.error(e1);
            } finally {
                pushHisForm.getPushHisCopyButton().setEnabled(true);
            }

        }));

        // 推送历史管理-导出按钮
        pushHisForm.getPushHisExportButton().addActionListener(e -> {
            int[] selectedRows = pushHisLeftTable.getSelectedRows();

            try {
                if (selectedRows.length > 0) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    int approve = fileChooser.showOpenDialog(pushHisPanel);
                    String exportPath;
                    if (approve == JFileChooser.APPROVE_OPTION) {
                        exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                    } else {
                        return;
                    }

                    for (int row : selectedRows) {
                        Integer selectedId = (Integer) pushHisLeftTable.getValueAt(row, 3);
                        TPushHistory tPushHistory = pushHistoryMapper.selectByPrimaryKey(selectedId);
                        File msgTemplateDataFile = new File(tPushHistory.getCsvFile());
                        if (msgTemplateDataFile.exists()) {
                            FileUtil.copy(msgTemplateDataFile.getAbsolutePath(), exportPath, true);
                        }
                    }
                    JOptionPane.showMessageDialog(pushHisPanel, "导出成功！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(new File(exportPath));
                    } catch (Exception e2) {
                        logger.error(e2);
                    }
                } else {
                    JOptionPane.showMessageDialog(pushHisPanel, "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }

            } catch (Exception e1) {
                JOptionPane.showMessageDialog(pushHisPanel, "导出失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }

        });

        // 重发
        pushHisForm.getResendFromHisButton().addActionListener(e -> ThreadUtil.execute(() -> {
            JProgressBar memberTabImportProgressBar = memberForm.getMemberTabImportProgressBar();
            int[] selectedRows = pushHisLeftTable.getSelectedRows();
            CSVReader reader = null;
            try {
                if (selectedRows.length > 0) {
                    MainWindow.getInstance().getTabbedPane().setSelectedIndex(3);
                    PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                    memberTabImportProgressBar.setVisible(true);
                    memberTabImportProgressBar.setIndeterminate(true);
                    for (int selectedRow : selectedRows) {
                        Integer selectedId = (Integer) pushHisLeftTable.getValueAt(selectedRow, 3);
                        TPushHistory tPushHistory = pushHistoryMapper.selectByPrimaryKey(selectedId);
                        File msgTemplateDataFile = new File(tPushHistory.getCsvFile());
                        if (msgTemplateDataFile.exists()) {
                            // 可以解决中文乱码问题
                            DataInputStream in = new DataInputStream(new FileInputStream(msgTemplateDataFile));
                            reader = new CSVReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                            String[] nextLine;
                            while ((nextLine = reader.readNext()) != null) {
                                PushData.allUser.add(nextLine);
                                memberForm.getMemberTabCountLabel().setText(String.valueOf(PushData.allUser.size()));
                            }
                        }
                    }
                    JOptionPane.showMessageDialog(pushHisPanel, "导入完成！", "完成",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(pushHisPanel, "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(pushHisPanel, "导入失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            } finally {
                memberTabImportProgressBar.setMaximum(100);
                memberTabImportProgressBar.setValue(100);
                memberTabImportProgressBar.setIndeterminate(false);
                memberTabImportProgressBar.setVisible(false);
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e1) {
                        logger.error(e1);
                        e1.printStackTrace();
                    }
                }
            }

        }));

    }

}