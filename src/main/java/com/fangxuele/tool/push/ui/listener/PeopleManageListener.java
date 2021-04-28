package com.fangxuele.tool.push.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.dao.TPeopleMapper;
import com.fangxuele.tool.push.ui.dialog.NewPeopleDialog;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.PeopleEditForm;
import com.fangxuele.tool.push.ui.form.PeopleManageForm;
import com.fangxuele.tool.push.util.MybatisUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * <pre>
 * 人群管理相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2021/3/23.
 */
public class PeopleManageListener {
    private static final Log logger = LogFactory.get();

    private static TPeopleMapper peopleMapper = MybatisUtil.getSqlSession().getMapper(TPeopleMapper.class);

    public static Integer selectedPeopleId;

    public static void addListeners() {
        PeopleManageForm peopleManageForm = PeopleManageForm.getInstance();

        JPanel mainPanel = MainWindow.getInstance().getMainPanel();

        JTable peopleListTable = peopleManageForm.getPeopleListTable();

        // 新建人群
        peopleManageForm.getCreatePeopleButton().addActionListener(e -> {
            NewPeopleDialog dialog = new NewPeopleDialog();
            dialog.pack();
            dialog.setVisible(true);
        });

        // 切换账号事件
        peopleManageForm.getAccountComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String accountName = e.getItem().toString();

                PeopleManageForm.initPeopleList();
            }
        });

        // 删除按钮事件
        peopleManageForm.getDeleteButton().addActionListener(e -> {
            try {
                int[] selectedRows = peopleListTable.getSelectedRows();
                if (selectedRows.length == 0) {
                    JOptionPane.showMessageDialog(mainPanel, "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    int isDelete = JOptionPane.showConfirmDialog(mainPanel, "确认删除？", "确认",
                            JOptionPane.YES_NO_OPTION);
                    if (isDelete == JOptionPane.YES_OPTION) {
                        DefaultTableModel tableModel = (DefaultTableModel) peopleListTable.getModel();

                        for (int i = 0; i < selectedRows.length; i++) {
                            int selectedRow = selectedRows[i];
                            Integer selectedId = (Integer) tableModel.getValueAt(selectedRow, 1);
                            peopleMapper.deleteByPrimaryKey(selectedId);
                        }

                        PeopleManageForm.initPeopleList();
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(mainPanel, "删除失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 点击左侧表格事件
        peopleListTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {

                int selectedRow = peopleListTable.getSelectedRow();
                selectedPeopleId = (int) peopleListTable.getValueAt(selectedRow, 1);

                PeopleEditForm.initDataTable(selectedPeopleId);

                super.mousePressed(e);
            }
        });
    }
}
