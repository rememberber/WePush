package com.fangxuele.tool.push.ui.form.msg;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgJiguangPush;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.component.TableInCellButtonColumn;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * <pre>
 * JiguangPushMsgForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/3.
 */
@Getter
public class JiguangPushMsgForm implements IMsgForm {
    private JPanel pushMsgPanel;
    private JComboBox<String> audienceTypeComboBox;
    private JCheckBox apnsProductionCheckBox;
    private JTextField titleTextField;
    private JTextArea contentTextArea;
    private JTextField extrasNameTextField;
    private JTextArea extrasValueTextField;
    private JButton extrasAddButton;
    private JTable extrasTable;

    /**
     * 目标类型值与下拉框选项的对应关系
     */
    private static final String[] AUDIENCE_TYPE_VALUES = {"alias", "registration_id"};

    private static JiguangPushMsgForm jiguangPushMsgForm;

    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    public JiguangPushMsgForm() {
        extrasAddButton.setIcon(new FlatSVGIcon("icon/add.svg"));

        // extras-添加 按钮事件
        extrasAddButton.addActionListener(e -> {
            String[] data = new String[2];
            data[0] = getInstance().getExtrasNameTextField().getText();
            data[1] = getInstance().getExtrasValueTextField().getText();

            if (getInstance().getExtrasTable().getModel().getRowCount() == 0) {
                initExtrasTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) getInstance().getExtrasTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();

            Set<String> keySet = new HashSet<>();
            String keyData;
            for (int i = 0; i < rowCount; i++) {
                keyData = (String) tableModel.getValueAt(i, 0);
                keySet.add(keyData);
            }

            if (StringUtils.isEmpty(data[0]) || StringUtils.isEmpty(data[1])) {
                JOptionPane.showMessageDialog(MessageEditForm.getInstance().getMsgEditorPanel(), "extras键和值不能为空！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (keySet.contains(data[0])) {
                JOptionPane.showMessageDialog(MessageEditForm.getInstance().getMsgEditorPanel(), "extras键不能重复！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                tableModel.addRow(data);
            }
        });
    }

    @Override
    public void init(Integer msgId) {
        clearAllField();
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        if (tMsg != null) {
            TMsgJiguangPush tMsgJiguangPush = JSONUtil.toBean(tMsg.getContent(), TMsgJiguangPush.class);
            JiguangPushMsgForm instance = getInstance();
            String audienceType = tMsgJiguangPush.getAudienceType();
            int audienceTypeIndex = 0;
            for (int i = 0; i < AUDIENCE_TYPE_VALUES.length; i++) {
                if (AUDIENCE_TYPE_VALUES[i].equals(audienceType)) {
                    audienceTypeIndex = i;
                    break;
                }
            }
            instance.getAudienceTypeComboBox().setSelectedIndex(audienceTypeIndex);
            instance.getApnsProductionCheckBox().setSelected(tMsgJiguangPush.isApnsProduction());
            instance.getTitleTextField().setText(tMsgJiguangPush.getTitle());
            instance.getContentTextArea().setText(tMsgJiguangPush.getContent());

            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            messageEditForm.getMsgNameField().setText(tMsg.getMsgName());
            messageEditForm.getPreviewUserField().setText(tMsg.getPreviewUser());
        }

        initExtrasTable();
        // extras表
        List<TemplateData> extrasList;
        if (tMsg == null) {
            extrasList = new ArrayList<>();
        } else {
            TMsgJiguangPush tMsgJiguangPush = JSONUtil.toBean(tMsg.getContent(), TMsgJiguangPush.class);
            extrasList = tMsgJiguangPush.getExtrasList() == null ? new ArrayList<>() : tMsgJiguangPush.getExtrasList();
        }
        String[] headerNames = {"extras键", "值", "操作"};
        Object[][] cellData = new String[extrasList.size()][headerNames.length];
        for (int i = 0; i < extrasList.size(); i++) {
            TemplateData tTemplateData = extrasList.get(i);
            cellData[i][0] = tTemplateData.getName();
            cellData[i][1] = tTemplateData.getValue();
        }
        DefaultTableModel model = new DefaultTableModel(cellData, headerNames);
        getInstance().getExtrasTable().setModel(model);
        TableColumnModel tableColumnModel = getInstance().getExtrasTable().getColumnModel();
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(getInstance().getExtrasTable(), headerNames.length - 1));
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(getInstance().getExtrasTable(), headerNames.length - 1));

        // 设置列宽
        tableColumnModel.getColumn(2).setPreferredWidth(46);
        tableColumnModel.getColumn(2).setMaxWidth(46);
    }

    @Override
    public void save(Integer accountId, String msgName) {
        int msgId = 0;
        boolean existSameMsg = false;

        TMsg tMsg = msgMapper.selectByUnique(MessageTypeEnum.JIGUANG_PUSH_CODE, accountId, msgName);
        if (tMsg != null) {
            existSameMsg = true;
            msgId = tMsg.getId();
        }

        int isCover = JOptionPane.NO_OPTION;
        if (existSameMsg) {
            // 如果存在，是否覆盖
            isCover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(), "已经存在同名的历史消息，\n是否覆盖？", "确认",
                    JOptionPane.YES_NO_OPTION);
        }

        if (!existSameMsg || isCover == JOptionPane.YES_OPTION) {
            JiguangPushMsgForm instance = getInstance();

            String now = SqliteUtil.nowDateForSqlite();

            TMsg msg = new TMsg();
            TMsgJiguangPush tMsgJiguangPush = new TMsgJiguangPush();
            msg.setMsgType(MessageTypeEnum.JIGUANG_PUSH_CODE);
            msg.setAccountId(accountId);
            msg.setMsgName(msgName);
            tMsgJiguangPush.setAudienceType(AUDIENCE_TYPE_VALUES[instance.getAudienceTypeComboBox().getSelectedIndex()]);
            tMsgJiguangPush.setApnsProduction(instance.getApnsProductionCheckBox().isSelected());
            tMsgJiguangPush.setTitle(instance.getTitleTextField().getText());
            tMsgJiguangPush.setContent(instance.getContentTextArea().getText());
            msg.setCreateTime(now);
            msg.setModifiedTime(now);
            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            msg.setPreviewUser(messageEditForm.getPreviewUserField().getText());

            // 如果table为空，则初始化
            if (instance.getExtrasTable().getModel().getRowCount() == 0) {
                initExtrasTable();
            }

            // 逐行读取
            DefaultTableModel tableModel = (DefaultTableModel) instance.getExtrasTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();
            List<TemplateData> extrasList = new ArrayList<>();
            for (int i = 0; i < rowCount; i++) {
                String name = (String) tableModel.getValueAt(i, 0);
                String value = (String) tableModel.getValueAt(i, 1);

                TemplateData tTemplateData = new TemplateData();
                tTemplateData.setName(name);
                tTemplateData.setValue(value);

                extrasList.add(tTemplateData);
            }

            tMsgJiguangPush.setExtrasList(extrasList);

            msg.setContent(JSONUtil.toJsonStr(tMsgJiguangPush));
            if (existSameMsg) {
                msg.setId(msgId);
                msgMapper.updateByPrimaryKeySelective(msg);
            } else {
                msgMapper.insertSelective(msg);
            }

            JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "保存成功！", "成功",
                    JOptionPane.INFORMATION_MESSAGE);
        }

    }

    public static JiguangPushMsgForm getInstance() {
        if (jiguangPushMsgForm == null) {
            jiguangPushMsgForm = new JiguangPushMsgForm();
        }
        return jiguangPushMsgForm;
    }

    /**
     * 初始化extras table
     */
    public static void initExtrasTable() {
        JTable extrasTable = getInstance().getExtrasTable();
        String[] headerNames = {"extras键", "值", "操作"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        extrasTable.setModel(model);
        extrasTable.updateUI();
        DefaultTableCellRenderer hr = (DefaultTableCellRenderer) extrasTable.getTableHeader().getDefaultRenderer();
        // 表头列名居左
        hr.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);

        TableColumnModel tableColumnModel = extrasTable.getColumnModel();
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(extrasTable, headerNames.length - 1));
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(extrasTable, headerNames.length - 1));

        // 设置列宽
        tableColumnModel.getColumn(2).setPreferredWidth(46);
        tableColumnModel.getColumn(2).setMaxWidth(46);
    }

    /**
     * 清空所有界面字段
     */
    @Override
    public void clearAllField() {
        getInstance().getAudienceTypeComboBox().setSelectedIndex(0);
        getInstance().getApnsProductionCheckBox().setSelected(true);
        getInstance().getTitleTextField().setText("");
        getInstance().getContentTextArea().setText("");
        getInstance().getExtrasNameTextField().setText("");
        getInstance().getExtrasValueTextField().setText("");
        initExtrasTable();
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
        pushMsgPanel = new JPanel();
        pushMsgPanel.setLayout(new GridLayoutManager(4, 1, new Insets(10, 5, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 5, 10, 5), -1, -1));
        pushMsgPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("目标类型");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        audienceTypeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("别名");
        defaultComboBoxModel1.addElement("RegistrationId");
        audienceTypeComboBox.setModel(defaultComboBoxModel1);
        audienceTypeComboBox.setToolTipText("推送目标标识的类型，人群文件中每行填写对应的别名或RegistrationId");
        panel1.add(audienceTypeComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        apnsProductionCheckBox = new JCheckBox();
        apnsProductionCheckBox.setSelected(true);
        apnsProductionCheckBox.setText("iOS生产环境");
        panel1.add(apnsProductionCheckBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 5, 10, 5), -1, -1));
        pushMsgPanel.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("通知标题");
        panel2.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        titleTextField = new JTextField();
        panel2.add(titleTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        pushMsgPanel.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "通知内容", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel3.getFont()), null));
        contentTextArea = new JTextArea();
        panel3.add(contentTextArea, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(0, 100), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(3, 3, new Insets(10, 0, 0, 0), -1, -1));
        pushMsgPanel.add(panel4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        panel4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "extras（自定义键值对，可选）", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel4.getFont()), null));
        final JLabel label3 = new JLabel();
        label3.setText("键");
        panel4.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("值");
        panel4.add(label4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        extrasNameTextField = new JTextField();
        panel4.add(extrasNameTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        extrasValueTextField = new JTextArea();
        panel4.add(extrasValueTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        extrasAddButton = new JButton();
        extrasAddButton.setText("");
        panel4.add(extrasAddButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        extrasTable = new JTable();
        extrasTable.setAutoCreateColumnsFromModel(true);
        extrasTable.setAutoCreateRowSorter(true);
        extrasTable.setGridColor(new Color(-12236470));
        extrasTable.setRowHeight(36);
        panel4.add(extrasTable, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        label1.setLabelFor(audienceTypeComboBox);
        label2.setLabelFor(titleTextField);
        label3.setLabelFor(extrasNameTextField);
        label4.setLabelFor(extrasValueTextField);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return pushMsgPanel;
    }

}
