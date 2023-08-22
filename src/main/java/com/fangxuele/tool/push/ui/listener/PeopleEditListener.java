package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TPeopleDataMapper;
import com.fangxuele.tool.push.dao.TPeopleMapper;
import com.fangxuele.tool.push.domain.TPeople;
import com.fangxuele.tool.push.domain.TPeopleData;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.PeopleImportWayEnum;
import com.fangxuele.tool.push.ui.dialog.ExportDialog;
import com.fangxuele.tool.push.ui.dialog.importway.*;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.PeopleEditForm;
import com.fangxuele.tool.push.ui.form.PeopleManageForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.opencsv.CSVWriter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * <pre>
 * 人群编辑相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2021/4/27.
 */
public class PeopleEditListener {
    private static final Log logger = LogFactory.get();

    private static TPeopleMapper peopleMapper = MybatisUtil.getSqlSession().getMapper(TPeopleMapper.class);
    private static TPeopleDataMapper peopleDataMapper = MybatisUtil.getSqlSession().getMapper(TPeopleDataMapper.class);

    public static void addListeners() {
        PeopleEditForm peopleEditForm = PeopleEditForm.getInstance();

        JPanel mainPanel = MainWindow.getInstance().getMainPanel();

        JTable memberListTable = peopleEditForm.getMemberListTable();

        // 导入按钮
        peopleEditForm.getImportButton().addActionListener(e -> {

            if (PeopleManageListener.selectedPeopleId == null) {
                JOptionPane.showMessageDialog(mainPanel, "请先选择一个人群!", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            TPeople tPeople = peopleMapper.selectByPrimaryKey(PeopleManageListener.selectedPeopleId);
            Integer msgType = tPeople.getMsgType();

            JPopupMenu popupMenu = new JPopupMenu();

            JMenuItem menuItem1 = new JMenuItem();
            menuItem1.setText(PeopleImportWayEnum.getName(PeopleImportWayEnum.BY_FILE));
            menuItem1.setIcon(new FlatSVGIcon("icon/file.svg"));
            menuItem1.addActionListener(e1 -> {
                String actionCommand = e1.getActionCommand();
                showImportDialog(actionCommand);
            });
            popupMenu.add(menuItem1);

            JMenuItem menuItem2 = new JMenuItem();
            menuItem2.setText(PeopleImportWayEnum.getName(PeopleImportWayEnum.BY_SQL));
            menuItem2.setIcon(new FlatSVGIcon("icon/data_base.svg"));
            menuItem2.addActionListener(e1 -> {
                String actionCommand = e1.getActionCommand();
                showImportDialog(actionCommand);
            });
            popupMenu.add(menuItem2);

            if (MessageTypeEnum.isWxMaOrMpType(msgType)) {
                JMenuItem menuItem3 = new JMenuItem();
                menuItem3.setText(PeopleImportWayEnum.getName(PeopleImportWayEnum.BY_WX_MP));
                menuItem3.setIcon(new FlatSVGIcon("icon/wechat.svg"));
                menuItem3.addActionListener(e1 -> {
                    String actionCommand = e1.getActionCommand();
                    showImportDialog(actionCommand);
                });
                popupMenu.add(menuItem3);
            }

            if (MessageTypeEnum.WX_CP_CODE == msgType) {
                JMenuItem menuItem4 = new JMenuItem();
                menuItem4.setText(PeopleImportWayEnum.getName(PeopleImportWayEnum.BY_WX_CP));
                menuItem4.setIcon(new FlatSVGIcon("icon/qiwei.svg"));
                menuItem4.addActionListener(e1 -> {
                    String actionCommand = e1.getActionCommand();
                    showImportDialog(actionCommand);
                });
                popupMenu.add(menuItem4);
            }

            if (MessageTypeEnum.DING_CODE == msgType) {
                JMenuItem menuItem5 = new JMenuItem();
                menuItem5.setText(PeopleImportWayEnum.getName(PeopleImportWayEnum.BY_DING));
                menuItem5.setIcon(new FlatSVGIcon("icon/dingding.svg"));
                menuItem5.addActionListener(e1 -> {
                    String actionCommand = e1.getActionCommand();
                    showImportDialog(actionCommand);
                });
                popupMenu.add(menuItem5);
            }

            if (MessageTypeEnum.HTTP_CODE == msgType) {
                JMenuItem menuItem6 = new JMenuItem();
                menuItem6.setText(PeopleImportWayEnum.getName(PeopleImportWayEnum.BY_NUM));
                menuItem6.setIcon(new FlatSVGIcon("icon/number.svg"));
                menuItem6.addActionListener(e1 -> {
                    String actionCommand = e1.getActionCommand();
                    showImportDialog(actionCommand);
                });
                popupMenu.add(menuItem6);
            }

            peopleEditForm.getImportButton().setComponentPopupMenu(popupMenu);

            peopleEditForm.getImportButton().getComponentPopupMenu().show(peopleEditForm.getImportButton(), -30, -peopleEditForm.getImportButton().getHeight() * popupMenu.getSubElements().length);
        });

        // 删除按钮
        peopleEditForm.getDeleteButton().addActionListener(e -> {
            try {
                int[] selectedRows = memberListTable.getSelectedRows();
                if (selectedRows.length == 0) {
                    JOptionPane.showMessageDialog(mainPanel, "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    int isDelete = JOptionPane.showConfirmDialog(mainPanel, "确认删除？", "确认",
                            JOptionPane.YES_NO_OPTION);
                    if (isDelete == JOptionPane.YES_OPTION) {
                        DefaultTableModel tableModel = (DefaultTableModel) memberListTable.getModel();

                        for (int i = 0; i < selectedRows.length; i++) {
                            int selectedRow = selectedRows[i];
                            Integer selectedId = (Integer) tableModel.getValueAt(selectedRow, 2);
                            peopleDataMapper.deleteByPrimaryKey(selectedId);
                        }

                        PeopleEditForm.initDataTable(PeopleManageListener.selectedPeopleId);
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(mainPanel, "删除失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 清空按钮
        peopleEditForm.getClearAllButton().addActionListener(e -> {

            try {
                if (PeopleManageListener.selectedPeopleId == null) {
                    JOptionPane.showMessageDialog(mainPanel, "请先选择一个人群!", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                int isDelete = JOptionPane.showConfirmDialog(mainPanel, "确认清空？", "确认",
                        JOptionPane.YES_NO_OPTION);
                if (isDelete == JOptionPane.YES_OPTION) {
                    peopleDataMapper.deleteByPeopleId(PeopleManageListener.selectedPeopleId);

                    PeopleEditForm.initDataTable(PeopleManageListener.selectedPeopleId);
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(mainPanel, "清空失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 导出按钮
        peopleEditForm.getExportButton().addActionListener(e -> ThreadUtil.execute(() -> {
            BigExcelWriter writer;
            try {
                if (PeopleManageListener.selectedPeopleId == null) {
                    JOptionPane.showMessageDialog(mainPanel, "请先选择一个人群!", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                List<TPeopleData> peopleDataList = peopleDataMapper.selectByPeopleId(PeopleManageListener.selectedPeopleId);
                if (peopleDataList.size() > 0) {
                    ExportDialog.showDialog();
                    if (ExportDialog.confirm) {
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        int approve = fileChooser.showOpenDialog(mainPanel);
                        String exportPath;
                        if (approve == JFileChooser.APPROVE_OPTION) {
                            exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                        } else {
                            return;
                        }

                        TPeople tPeople = peopleMapper.selectByPrimaryKey(PeopleManageListener.selectedPeopleId);

                        String nowTime = DateUtil.now().replace(":", "_").replace(" ", "_");
                        String fileName = "MemberExport_" + MessageTypeEnum.getName(App.config.getMsgType()) + "_" + tPeople.getPeopleName() + "_" + nowTime;
                        String fileFullName = exportPath + File.separator + fileName;
                        if (ExportDialog.fileType == ExportDialog.EXCEL) {
                            fileFullName += ".xlsx";
                            //通过工具类创建writer
                            writer = ExcelUtil.getBigWriter(fileFullName);
                            //合并单元格后的标题行，使用默认标题样式
                            writer.merge(JSONUtil.toList(peopleDataList.get(0).getVarData(), String.class).size() - 1, "人群数据列表导出");
                            //一次性写出内容，强制输出标题
                            for (TPeopleData tPeopleData : peopleDataList) {
                                String varData = tPeopleData.getVarData();
                                writer.writeRow(JSONUtil.toList(varData, String.class));
                            }
                            writer.flush();
                        } else if (ExportDialog.fileType == ExportDialog.CSV) {
                            fileFullName += ".csv";
                            CSVWriter csvWriter = new CSVWriter(new FileWriter(FileUtil.touch(fileFullName)));

                            for (TPeopleData tPeopleData : peopleDataList) {
                                String varData = tPeopleData.getVarData();
                                JSONArray jsonArray = JSONUtil.parseArray(varData);
                                String[] nextLine = new String[jsonArray.size()];
                                nextLine = jsonArray.toArray(nextLine);
                                csvWriter.writeNext(nextLine);
                            }

                            csvWriter.flush();
                            csvWriter.close();
                        } else if (ExportDialog.fileType == ExportDialog.TXT) {
                            fileFullName += ".txt";
                            FileWriter fileWriter = new FileWriter(fileFullName);

                            int size = peopleDataList.size();
                            for (int i = 0; i < size; i++) {
                                String varData = peopleDataList.get(i).getVarData();
                                List<String> row = JSONUtil.toList(varData, String.class);
                                fileWriter.append(String.join("|", row));
                                if (i < size - 1) {
                                    fileWriter.append(StrUtil.CRLF);
                                }
                            }

                            fileWriter.flush();
                            fileWriter.close();
                        }
                        JOptionPane.showMessageDialog(mainPanel, "导出成功！", "提示",
                                JOptionPane.INFORMATION_MESSAGE);
                        try {
                            Desktop desktop = Desktop.getDesktop();
                            desktop.open(FileUtil.file(fileFullName));
                        } catch (Exception e2) {
                            logger.error(e2);
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(mainPanel, "所选人群无数据", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(mainPanel, "导出失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        }));

        // 搜索按钮
        peopleEditForm.getSearchButton().addActionListener(e -> searchEvent());

        // 搜索框键入回车
        peopleEditForm.getSearchTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                try {
                    searchEvent();
                } catch (Exception e1) {
                    logger.error(e1);
                } finally {
                    super.keyPressed(e);
                }
            }
        });

    }

    /**
     * 弹出导入对话框
     *
     * @param actionCommand
     */
    private static void showImportDialog(String actionCommand) {
        String selectedAccountName = PeopleManageForm.getInstance().getAccountComboBox().getSelectedItem().toString();

        if (PeopleImportWayEnum.getName(PeopleImportWayEnum.BY_FILE).equals(actionCommand)) {
            ImportByFile dialog = new ImportByFile(PeopleManageListener.selectedPeopleId);
            dialog.pack();
            dialog.setVisible(true);
        } else if (PeopleImportWayEnum.getName(PeopleImportWayEnum.BY_SQL).equals(actionCommand)) {
            ImportBySQL dialog = new ImportBySQL(PeopleManageListener.selectedPeopleId);
            dialog.pack();
            dialog.setVisible(true);
        } else if (PeopleImportWayEnum.getName(PeopleImportWayEnum.BY_WX_MP).equals(actionCommand)) {
            ImportByWxMp dialog = new ImportByWxMp(PeopleManageListener.selectedPeopleId);
            dialog.pack();
            dialog.setVisible(true);
        } else if (PeopleImportWayEnum.getName(PeopleImportWayEnum.BY_WX_CP).equals(actionCommand)) {
            ImportByWxCp dialog = new ImportByWxCp(PeopleManageListener.selectedPeopleId);
            dialog.pack();
            dialog.setVisible(true);
        } else if (PeopleImportWayEnum.getName(PeopleImportWayEnum.BY_DING).equals(actionCommand)) {
            ImportByDing dialog = new ImportByDing(PeopleManageListener.selectedPeopleId);
            dialog.pack();
            dialog.setVisible(true);
        } else if (PeopleImportWayEnum.getName(PeopleImportWayEnum.BY_NUM).equals(actionCommand)) {
            ImportByNum dialog = new ImportByNum(PeopleManageListener.selectedPeopleId);
            dialog.pack();
            dialog.setVisible(true);
        }
    }

    /**
     * 搜索
     */
    private static void searchEvent() {
        try {
            String keyWord = PeopleEditForm.getInstance().getSearchTextField().getText();
            List<TPeopleData> peopleDataList = peopleDataMapper.selectByPeopleIdAndKeyword(PeopleManageListener.selectedPeopleId, "%" + keyWord + "%");
            PeopleEditForm.initPeopleDataTable(peopleDataList);
        } catch (Exception e1) {
            logger.error(e1);
        }
    }
}
