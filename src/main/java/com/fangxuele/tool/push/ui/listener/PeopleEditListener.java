package com.fangxuele.tool.push.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.dao.TPeopleMapper;
import com.fangxuele.tool.push.ui.dialog.importway.*;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.PeopleEditForm;
import com.fangxuele.tool.push.util.MybatisUtil;

import javax.swing.*;

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

    public static void addListeners() {
        PeopleEditForm peopleEditForm = PeopleEditForm.getInstance();

        JPanel mainPanel = MainWindow.getInstance().getMainPanel();

        // 导入按钮
        peopleEditForm.getImportButton().addActionListener(e -> {

            if (PeopleManageListener.selectedPeopleId == null) {
                JOptionPane.showMessageDialog(mainPanel, "请先选择一个人群!", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JPopupMenu popupMenu = new JPopupMenu();

            JMenuItem menuItem1 = new JMenuItem();
            menuItem1.setText("通过文件导入");
            menuItem1.addActionListener(e1 -> {
                String actionCommand = e1.getActionCommand();
                showImportDialog(actionCommand);
            });
            popupMenu.add(menuItem1);

            JMenuItem menuItem2 = new JMenuItem();
            menuItem2.setText("通过SQL导入");
            menuItem2.addActionListener(e1 -> {
                String actionCommand = e1.getActionCommand();
                showImportDialog(actionCommand);
            });
            popupMenu.add(menuItem2);

            JMenuItem menuItem3 = new JMenuItem();
            menuItem3.setText("通过微信公众平台导入");
            menuItem3.addActionListener(e1 -> {
                String actionCommand = e1.getActionCommand();
                showImportDialog(actionCommand);
            });
            popupMenu.add(menuItem3);

            JMenuItem menuItem4 = new JMenuItem();
            menuItem4.setText("通过微信企业通讯录导入");
            menuItem4.addActionListener(e1 -> {
                String actionCommand = e1.getActionCommand();
                showImportDialog(actionCommand);
            });
            popupMenu.add(menuItem4);

            JMenuItem menuItem5 = new JMenuItem();
            menuItem5.setText("通过钉钉通讯录导入");
            menuItem5.addActionListener(e1 -> {
                String actionCommand = e1.getActionCommand();
                showImportDialog(actionCommand);
            });
            popupMenu.add(menuItem5);

            JMenuItem menuItem6 = new JMenuItem();
            menuItem6.setText("通过数量导入");
            menuItem6.addActionListener(e1 -> {
                String actionCommand = e1.getActionCommand();
                showImportDialog(actionCommand);
            });
            popupMenu.add(menuItem6);

            peopleEditForm.getImportButton().setComponentPopupMenu(popupMenu);
            peopleEditForm.getImportButton().getComponentPopupMenu().show(peopleEditForm.getImportButton(), -peopleEditForm.getImportButton().getWidth(), -peopleEditForm.getImportButton().getHeight() * 5);
        });
    }

    private static void showImportDialog(String actionCommand) {
        if ("通过文件导入".equals(actionCommand)) {
            ImportByFile dialog = new ImportByFile();
            dialog.pack();
            dialog.setVisible(true);
        } else if ("通过SQL导入".equals(actionCommand)) {
            ImportBySQL dialog = new ImportBySQL();
            dialog.pack();
            dialog.setVisible(true);
        } else if ("通过微信公众平台导入".equals(actionCommand)) {
            ImportByWxMp dialog = new ImportByWxMp();
            dialog.pack();
            dialog.setVisible(true);
        } else if ("通过微信企业通讯录导入".equals(actionCommand)) {
            ImportByWxCp dialog = new ImportByWxCp();
            dialog.pack();
            dialog.setVisible(true);
        } else if ("通过钉钉通讯录导入".equals(actionCommand)) {
            ImportByDing dialog = new ImportByDing();
            dialog.pack();
            dialog.setVisible(true);
        } else if ("通过数量导入".equals(actionCommand)) {
            ImportByNum dialog = new ImportByNum();
            dialog.pack();
            dialog.setVisible(true);
        }
    }
}
