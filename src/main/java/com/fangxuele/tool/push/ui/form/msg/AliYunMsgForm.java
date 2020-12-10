package com.fangxuele.tool.push.ui.form.msg;

import com.fangxuele.tool.push.dao.TMsgSmsMapper;
import com.fangxuele.tool.push.dao.TTemplateDataMapper;
import com.fangxuele.tool.push.domain.TMsgSms;
import com.fangxuele.tool.push.domain.TTemplateData;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.component.TableInCellButtonColumn;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * <pre>
 * AliYunMsgForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/3.
 */
@Getter
public class AliYunMsgForm implements IMsgForm {
    private JPanel templateMsgPanel;
    private JLabel templateIdLabel;
    private JTextField msgTemplateIdTextField;
    private JPanel templateMsgDataPanel;
    private JLabel templateMsgNameLabel;
    private JTextField templateDataNameTextField;
    private JLabel templateMsgValueLabel;
    private JTextField templateDataValueTextField;
    private JButton templateMsgDataAddButton;
    private JTable templateMsgDataTable;

    private static AliYunMsgForm aliYunMsgForm;

    private static TMsgSmsMapper msgSmsMapper = MybatisUtil.getSqlSession().getMapper(TMsgSmsMapper.class);
    private static TTemplateDataMapper templateDataMapper = MybatisUtil.getSqlSession().getMapper(TTemplateDataMapper.class);

    public AliYunMsgForm() {
        // 模板数据-添加 按钮事件
        templateMsgDataAddButton.addActionListener(e -> {
            String[] data = new String[2];
            data[0] = getInstance().getTemplateDataNameTextField().getText();
            data[1] = getInstance().getTemplateDataValueTextField().getText();

            if (getInstance().getTemplateMsgDataTable().getModel().getRowCount() == 0) {
                initTemplateDataTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) getInstance().getTemplateMsgDataTable().getModel();
            int rowCount = tableModel.getRowCount();

            Set<String> keySet = new HashSet<>();
            String keyData;
            for (int i = 0; i < rowCount; i++) {
                keyData = (String) tableModel.getValueAt(i, 0);
                keySet.add(keyData);
            }

            if (StringUtils.isEmpty(data[0]) || StringUtils.isEmpty(data[1])) {
                JOptionPane.showMessageDialog(MessageEditForm.getInstance().getMsgEditorPanel(), "模板参数和参数对应的值不能为空！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (keySet.contains(data[0])) {
                JOptionPane.showMessageDialog(MessageEditForm.getInstance().getMsgEditorPanel(), "模板参数不能重复！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                tableModel.addRow(data);
            }
        });
    }

    @Override
    public void init(String msgName) {
        clearAllField();
        List<TMsgSms> tMsgSmsList = msgSmsMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.ALI_YUN_CODE, msgName);
        Integer msgId = 0;
        if (tMsgSmsList.size() > 0) {
            TMsgSms tMsgSms = tMsgSmsList.get(0);
            msgId = tMsgSms.getId();
            getInstance().getMsgTemplateIdTextField().setText(tMsgSms.getTemplateId());
            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            messageEditForm.getMsgNameField().setText(tMsgSms.getMsgName());
            messageEditForm.getPreviewUserField().setText(tMsgSms.getPreviewUser());
        }

        initTemplateDataTable();
        // 模板消息Data表
        List<TTemplateData> templateDataList = templateDataMapper.selectByMsgTypeAndMsgId(MessageTypeEnum.ALI_YUN_CODE, msgId);
        String[] headerNames = {"模板参数", "参数对应的值", "操作"};
        Object[][] cellData = new String[templateDataList.size()][headerNames.length];
        for (int i = 0; i < templateDataList.size(); i++) {
            TTemplateData tTemplateData = templateDataList.get(i);
            cellData[i][0] = tTemplateData.getName();
            cellData[i][1] = tTemplateData.getValue();
        }
        DefaultTableModel model = new DefaultTableModel(cellData, headerNames);
        getInstance().getTemplateMsgDataTable().setModel(model);
        TableColumnModel tableColumnModel = getInstance().getTemplateMsgDataTable().getColumnModel();
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(getInstance().getTemplateMsgDataTable(), headerNames.length - 1));
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(getInstance().getTemplateMsgDataTable(), headerNames.length - 1));

        // 设置列宽
        tableColumnModel.getColumn(2).setPreferredWidth(46);
        tableColumnModel.getColumn(2).setMaxWidth(46);
    }

    @Override
    public void save(String msgName) {
        int msgId = 0;
        boolean existSameMsg = false;

        List<TMsgSms> tMsgSmsList = msgSmsMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.ALI_YUN_CODE, msgName);
        if (tMsgSmsList.size() > 0) {
            existSameMsg = true;
            msgId = tMsgSmsList.get(0).getId();
        }

        int isCover = JOptionPane.NO_OPTION;
        if (existSameMsg) {
            // 如果存在，是否覆盖
            isCover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(), "已经存在同名的历史消息，\n是否覆盖？", "确认",
                    JOptionPane.YES_NO_OPTION);
        }

        if (!existSameMsg || isCover == JOptionPane.YES_OPTION) {
            String templateId = getInstance().getMsgTemplateIdTextField().getText();

            String now = SqliteUtil.nowDateForSqlite();

            TMsgSms tMsgSms = new TMsgSms();
            tMsgSms.setMsgType(MessageTypeEnum.ALI_YUN_CODE);
            tMsgSms.setMsgName(msgName);
            tMsgSms.setTemplateId(templateId);
            tMsgSms.setCreateTime(now);
            tMsgSms.setModifiedTime(now);
            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            tMsgSms.setPreviewUser(messageEditForm.getPreviewUserField().getText());

            if (existSameMsg) {
                msgSmsMapper.updateByMsgTypeAndMsgName(tMsgSms);
            } else {
                msgSmsMapper.insertSelective(tMsgSms);
                msgId = tMsgSms.getId();
            }

            // 保存模板数据
            // 如果是覆盖保存，则先清空之前的模板数据
            if (existSameMsg) {
                templateDataMapper.deleteByMsgTypeAndMsgId(MessageTypeEnum.ALI_YUN_CODE, msgId);
            }

            // 如果table为空，则初始化
            if (getInstance().getTemplateMsgDataTable().getModel().getRowCount() == 0) {
                initTemplateDataTable();
            }

            // 逐行读取
            DefaultTableModel tableModel = (DefaultTableModel) getInstance().getTemplateMsgDataTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                String name = (String) tableModel.getValueAt(i, 0);
                String value = (String) tableModel.getValueAt(i, 1);

                TTemplateData tTemplateData = new TTemplateData();
                tTemplateData.setMsgType(MessageTypeEnum.ALI_YUN_CODE);
                tTemplateData.setMsgId(msgId);
                tTemplateData.setName(name);
                tTemplateData.setValue(value);
                tTemplateData.setCreateTime(now);
                tTemplateData.setModifiedTime(now);

                templateDataMapper.insert(tTemplateData);
            }

            JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "保存成功！", "成功",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static AliYunMsgForm getInstance() {
        if (aliYunMsgForm == null) {
            aliYunMsgForm = new AliYunMsgForm();
        }
        return aliYunMsgForm;
    }

    /**
     * 初始化模板消息数据table
     */
    public static void initTemplateDataTable() {
        JTable msgDataTable = getInstance().getTemplateMsgDataTable();
        String[] headerNames = {"模板参数", "参数对应的值", "操作"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        msgDataTable.setModel(model);
        msgDataTable.updateUI();
        DefaultTableCellRenderer hr = (DefaultTableCellRenderer) msgDataTable.getTableHeader().getDefaultRenderer();
        // 表头列名居左
        hr.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);

        TableColumnModel tableColumnModel = msgDataTable.getColumnModel();
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(msgDataTable, headerNames.length - 1));
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(msgDataTable, headerNames.length - 1));

        // 设置列宽
        tableColumnModel.getColumn(2).setPreferredWidth(46);
        tableColumnModel.getColumn(2).setMaxWidth(46);
    }

    /**
     * 清空所有界面字段
     */
    public static void clearAllField() {
        getInstance().getMsgTemplateIdTextField().setText("");
        getInstance().getTemplateDataNameTextField().setText("");
        getInstance().getTemplateDataValueTextField().setText("");
        initTemplateDataTable();
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
        templateMsgPanel = new JPanel();
        templateMsgPanel.setLayout(new GridLayoutManager(2, 1, new Insets(10, 5, 0, 0), -1, -1));
        panel1.add(templateMsgPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        templateMsgDataPanel = new JPanel();
        templateMsgDataPanel.setLayout(new GridLayoutManager(3, 3, new Insets(10, 0, 0, 0), -1, -1));
        templateMsgPanel.add(templateMsgDataPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        templateMsgDataPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "短信模板变量（可使用\"${ENTER}\"作为换行符）", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, -1, -1, templateMsgDataPanel.getFont()), null));
        templateDataNameTextField = new JTextField();
        templateDataNameTextField.setToolTipText("当消息类型是模板消息时的示例：first或者keyword1或者remark之类的");
        templateMsgDataPanel.add(templateDataNameTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        templateDataValueTextField = new JTextField();
        templateMsgDataPanel.add(templateDataValueTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        templateMsgDataAddButton = new JButton();
        templateMsgDataAddButton.setIcon(new ImageIcon(getClass().getResource("/icon/add.png")));
        templateMsgDataAddButton.setText("");
        templateMsgDataPanel.add(templateMsgDataAddButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        templateMsgDataTable = new JTable();
        templateMsgDataTable.setAutoCreateColumnsFromModel(true);
        templateMsgDataTable.setAutoCreateRowSorter(true);
        templateMsgDataTable.setGridColor(new Color(-12236470));
        templateMsgDataTable.setRowHeight(36);
        templateMsgDataPanel.add(templateMsgDataTable, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        templateMsgNameLabel = new JLabel();
        templateMsgNameLabel.setText("模板参数");
        templateMsgNameLabel.setToolTipText("当消息类型是模板消息时的示例：first或者keyword1或者remark之类的");
        templateMsgDataPanel.add(templateMsgNameLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        templateMsgValueLabel = new JLabel();
        templateMsgValueLabel.setText("参数对应的值");
        templateMsgDataPanel.add(templateMsgValueLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 10, 0), -1, -1));
        templateMsgPanel.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        templateIdLabel = new JLabel();
        templateIdLabel.setText("短信模板ID *");
        panel2.add(templateIdLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgTemplateIdTextField = new JTextField();
        panel2.add(msgTemplateIdTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        templateMsgNameLabel.setLabelFor(templateDataNameTextField);
        templateMsgValueLabel.setLabelFor(templateDataValueTextField);
        templateIdLabel.setLabelFor(msgTemplateIdTextField);
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

}
