package com.fangxuele.tool.push.ui.form;

import com.fangxuele.tool.push.dao.TMsgKefuMapper;
import com.fangxuele.tool.push.dao.TMsgKefuPriorityMapper;
import com.fangxuele.tool.push.dao.TMsgMaTemplateMapper;
import com.fangxuele.tool.push.dao.TMsgMpTemplateMapper;
import com.fangxuele.tool.push.dao.TMsgSmsMapper;
import com.fangxuele.tool.push.domain.TMsgKefu;
import com.fangxuele.tool.push.domain.TMsgKefuPriority;
import com.fangxuele.tool.push.domain.TMsgMaTemplate;
import com.fangxuele.tool.push.domain.TMsgMpTemplate;
import com.fangxuele.tool.push.domain.TMsgSms;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.component.TableInCellCheckBoxRenderer;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * <pre>
 * MessageManageForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/5/6.
 */
@Getter
public class MessageManageForm {

    private JPanel messageManagePanel;
    private JTable msgHistable;
    private JButton msgHisTableSelectAllButton;
    private JButton msgHisTableDeleteButton;
    private JButton createMsgButton;

    public static MessageManageForm messageManageForm = new MessageManageForm();

    private static TMsgKefuMapper msgKefuMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuMapper.class);
    private static TMsgKefuPriorityMapper msgKefuPriorityMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuPriorityMapper.class);
    private static TMsgMaTemplateMapper msgMaTemplateMapper = MybatisUtil.getSqlSession().getMapper(TMsgMaTemplateMapper.class);
    private static TMsgMpTemplateMapper msgMpTemplateMapper = MybatisUtil.getSqlSession().getMapper(TMsgMpTemplateMapper.class);
    private static TMsgSmsMapper msgSmsMapper = MybatisUtil.getSqlSession().getMapper(TMsgSmsMapper.class);

    /**
     * 初始化消息列表
     */
    public static void init() {
        // 历史消息管理
        String[] headerNames = {"选择", "消息名称"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        messageManageForm.getMsgHistable().setModel(model);
        // 隐藏表头
        messageManageForm.getMsgHistable().getTableHeader().setVisible(false);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setPreferredSize(new Dimension(0, 0));
        messageManageForm.getMsgHistable().getTableHeader().setDefaultRenderer(renderer);

        int msgType = Init.config.getMsgType();

        Object[] data;

        if (msgType == MessageTypeEnum.KEFU_CODE) {
            List<TMsgKefu> tMsgKefuList = msgKefuMapper.selectByMsgType(msgType);
            for (TMsgKefu tMsgKefu : tMsgKefuList) {
                data = new Object[2];
                data[0] = false;
                data[1] = tMsgKefu.getMsgName();
                model.addRow(data);
            }
        } else if (msgType == MessageTypeEnum.KEFU_PRIORITY_CODE) {
            List<TMsgKefuPriority> tMsgKefuPriorityList = msgKefuPriorityMapper.selectByMsgType(msgType);
            for (TMsgKefuPriority tMsgKefuPriority : tMsgKefuPriorityList) {
                data = new Object[2];
                data[0] = false;
                data[1] = tMsgKefuPriority.getMsgName();
                model.addRow(data);
            }
        } else if (msgType == MessageTypeEnum.MA_TEMPLATE_CODE) {
            List<TMsgMaTemplate> tMsgMaTemplateList = msgMaTemplateMapper.selectByMsgType(msgType);
            for (TMsgMaTemplate tMsgMaTemplate : tMsgMaTemplateList) {
                data = new Object[2];
                data[0] = false;
                data[1] = tMsgMaTemplate.getMsgName();
                model.addRow(data);
            }
        } else if (msgType == MessageTypeEnum.MP_TEMPLATE_CODE) {
            List<TMsgMpTemplate> tMsgMpTemplateList = msgMpTemplateMapper.selectByMsgType(msgType);
            for (TMsgMpTemplate tMsgMpTemplate : tMsgMpTemplateList) {
                data = new Object[2];
                data[0] = false;
                data[1] = tMsgMpTemplate.getMsgName();
                model.addRow(data);
            }
        } else {
            List<TMsgSms> tMsgSmsList = msgSmsMapper.selectByMsgType(msgType);
            for (TMsgSms tMsgSms : tMsgSmsList) {
                data = new Object[2];
                data[0] = false;
                data[1] = tMsgSms.getMsgName();
                model.addRow(data);
            }
        }

        messageManageForm.getMsgHistable().getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        messageManageForm.getMsgHistable().getColumnModel().getColumn(0).setCellRenderer(new TableInCellCheckBoxRenderer());
        // 设置列宽
        messageManageForm.getMsgHistable().getColumnModel().getColumn(0).setPreferredWidth(50);
        messageManageForm.getMsgHistable().getColumnModel().getColumn(0).setMaxWidth(50);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        messageManagePanel = new JPanel();
        messageManagePanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        messageManagePanel.setMaximumSize(new Dimension(-1, -1));
        messageManagePanel.setMinimumSize(new Dimension(-1, -1));
        messageManagePanel.setPreferredSize(new Dimension(280, -1));
        panel1.add(messageManagePanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        messageManagePanel.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        msgHistable = new JTable();
        msgHistable.setGridColor(new Color(-12236470));
        msgHistable.setRowHeight(40);
        scrollPane1.setViewportView(msgHistable);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        messageManagePanel.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        msgHisTableSelectAllButton = new JButton();
        msgHisTableSelectAllButton.setText("全选");
        panel2.add(msgHisTableSelectAllButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgHisTableDeleteButton = new JButton();
        msgHisTableDeleteButton.setText("删除");
        panel2.add(msgHisTableDeleteButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        createMsgButton = new JButton();
        createMsgButton.setText("新建");
        panel2.add(createMsgButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }
}
