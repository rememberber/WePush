package com.fangxuele.tool.push.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.dao.TPeopleMapper;
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
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem menuItem1 = new JMenuItem();
            menuItem1.setText("通过文件导入");
            menuItem1.addActionListener(e1 -> {
                String actionCommand = e1.getActionCommand();
                System.err.println(actionCommand);
            });
            popupMenu.add(menuItem1);
            JMenuItem menuItem2 = new JMenuItem();
            menuItem2.setText("通过SQL导入");
            popupMenu.add(menuItem2);
            JMenuItem menuItem3 = new JMenuItem();
            menuItem3.setText("通过微信公众平台导入");
            popupMenu.add(menuItem3);
            peopleEditForm.getImportButton().setComponentPopupMenu(popupMenu);
            peopleEditForm.getImportButton().getComponentPopupMenu().show(peopleEditForm.getImportButton(), -peopleEditForm.getImportButton().getWidth(), -peopleEditForm.getImportButton().getHeight() * 3);
        });
    }
}
