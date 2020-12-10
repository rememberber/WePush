package com.fangxuele.tool.push.ui.form.msg;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TMsgMaSubscribeMapper;
import com.fangxuele.tool.push.dao.TTemplateDataMapper;
import com.fangxuele.tool.push.domain.TMsgMaSubscribe;
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
 * MaSubscribeMsgForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/11/29.
 */
@Getter
public class MaSubscribeMsgForm implements IMsgForm {
    private JPanel templateMsgPanel;
    private JLabel templateIdLabel;
    private JTextField msgTemplateIdTextField;
    private JLabel templateUrlLabel;
    private JTextField msgTemplateUrlTextField;
    private JPanel templateMsgDataPanel;
    private JLabel templateMsgNameLabel;
    private JTextField templateDataNameTextField;
    private JLabel templateMsgValueLabel;
    private JTextField templateDataValueTextField;
    private JLabel templateMsgColorLabel;
    private JTextField templateDataColorTextField;
    private JButton templateMsgDataAddButton;
    private JTable templateMsgDataTable;

    private static MaSubscribeMsgForm maSubscribeMsgForm;

    private static TMsgMaSubscribeMapper msgMaSubscribeMapper = MybatisUtil.getSqlSession().getMapper(TMsgMaSubscribeMapper.class);
    private static TTemplateDataMapper templateDataMapper = MybatisUtil.getSqlSession().getMapper(TTemplateDataMapper.class);

    public MaSubscribeMsgForm() {
        // 模板数据-添加 按钮事件
        templateMsgDataAddButton.addActionListener(e -> {
            String[] data = new String[3];
            data[0] = getInstance().getTemplateDataNameTextField().getText();
            data[1] = getInstance().getTemplateDataValueTextField().getText();
            data[2] = getInstance().getTemplateDataColorTextField().getText();

            if (getInstance().getTemplateMsgDataTable().getModel().getRowCount() == 0) {
                initTemplateDataTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) getInstance().getTemplateMsgDataTable()
                    .getModel();
            int rowCount = tableModel.getRowCount();

            Set<String> keySet = new HashSet<>();
            String keyData;
            for (int i = 0; i < rowCount; i++) {
                keyData = (String) tableModel.getValueAt(i, 0);
                keySet.add(keyData);
            }

            if (StringUtils.isEmpty(data[0]) || StringUtils.isEmpty(data[1])) {
                JOptionPane.showMessageDialog(MessageEditForm.getInstance().getMsgEditorPanel(), "Name或value不能为空！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (keySet.contains(data[0])) {
                JOptionPane.showMessageDialog(MessageEditForm.getInstance().getMsgEditorPanel(), "Name不能重复！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                if (StringUtils.isEmpty(data[2])) {
                    data[2] = "#000000";
                } else if (!data[2].startsWith("#")) {
                    data[2] = "#" + data[2];
                }
                tableModel.addRow(data);
            }
        });
    }

    @Override
    public void init(String msgName) {
        clearAllField();
        List<TMsgMaSubscribe> tMsgMaSubscribeList = msgMaSubscribeMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.MA_SUBSCRIBE_CODE, msgName);
        Integer msgId = 0;
        if (tMsgMaSubscribeList.size() > 0) {
            TMsgMaSubscribe tMsgMaSubscribe = tMsgMaSubscribeList.get(0);
            msgId = tMsgMaSubscribe.getId();
            getInstance().getMsgTemplateIdTextField().setText(tMsgMaSubscribe.getTemplateId());
            getInstance().getMsgTemplateUrlTextField().setText(tMsgMaSubscribe.getPage());

            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            messageEditForm.getMsgNameField().setText(tMsgMaSubscribe.getMsgName());
            messageEditForm.getPreviewUserField().setText(tMsgMaSubscribe.getPreviewUser());
        }

        initTemplateDataTable();
        // 模板消息Data表
        List<TTemplateData> templateDataList = templateDataMapper.selectByMsgTypeAndMsgId(MessageTypeEnum.MA_SUBSCRIBE_CODE, msgId);
        String[] headerNames = {"Name", "Value", "Color", "操作"};
        Object[][] cellData = new String[templateDataList.size()][headerNames.length];
        for (int i = 0; i < templateDataList.size(); i++) {
            TTemplateData tTemplateData = templateDataList.get(i);
            cellData[i][0] = tTemplateData.getName();
            cellData[i][1] = tTemplateData.getValue();
            cellData[i][2] = tTemplateData.getColor();
        }
        DefaultTableModel model = new DefaultTableModel(cellData, headerNames);
        getInstance().getTemplateMsgDataTable().setModel(model);
        TableColumnModel tableColumnModel = getInstance().getTemplateMsgDataTable().getColumnModel();
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(getInstance().getTemplateMsgDataTable(), headerNames.length - 1));
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(getInstance().getTemplateMsgDataTable(), headerNames.length - 1));

        // 设置列宽
        tableColumnModel.getColumn(3).setPreferredWidth(46);
        tableColumnModel.getColumn(3).setMaxWidth(46);
    }

    @Override
    public void save(String msgName) {
        int msgId = 0;
        boolean existSameMsg = false;

        List<TMsgMaSubscribe> tMsgMaSubscribeList = msgMaSubscribeMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.MA_SUBSCRIBE_CODE, msgName);
        if (tMsgMaSubscribeList.size() > 0) {
            existSameMsg = true;
            msgId = tMsgMaSubscribeList.get(0).getId();
        }

        int isCover = JOptionPane.NO_OPTION;
        if (existSameMsg) {
            // 如果存在，是否覆盖
            isCover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(), "已经存在同名的历史消息，\n是否覆盖？", "确认",
                    JOptionPane.YES_NO_OPTION);
        }

        if (!existSameMsg || isCover == JOptionPane.YES_OPTION) {
            String templateId = getInstance().getMsgTemplateIdTextField().getText();
            String templateUrl = getInstance().getMsgTemplateUrlTextField().getText();

            String now = SqliteUtil.nowDateForSqlite();

            TMsgMaSubscribe tMsgMaSubscribe = new TMsgMaSubscribe();
            tMsgMaSubscribe.setMsgType(MessageTypeEnum.MA_SUBSCRIBE_CODE);
            tMsgMaSubscribe.setMsgName(msgName);
            tMsgMaSubscribe.setTemplateId(templateId);
            tMsgMaSubscribe.setPage(templateUrl);
            tMsgMaSubscribe.setCreateTime(now);
            tMsgMaSubscribe.setModifiedTime(now);

            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            tMsgMaSubscribe.setPreviewUser(messageEditForm.getPreviewUserField().getText());
            tMsgMaSubscribe.setWxAccountId(App.config.getWxAccountId());

            if (existSameMsg) {
                msgMaSubscribeMapper.updateByMsgTypeAndMsgName(tMsgMaSubscribe);
            } else {
                msgMaSubscribeMapper.insertSelective(tMsgMaSubscribe);
                msgId = tMsgMaSubscribe.getId();
            }

            // 保存模板数据

            // 如果是覆盖保存，则先清空之前的模板数据
            if (existSameMsg) {
                templateDataMapper.deleteByMsgTypeAndMsgId(MessageTypeEnum.MA_SUBSCRIBE_CODE, msgId);
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
                String color = ((String) tableModel.getValueAt(i, 2)).trim();

                TTemplateData tTemplateData = new TTemplateData();
                tTemplateData.setMsgType(MessageTypeEnum.MA_SUBSCRIBE_CODE);
                tTemplateData.setMsgId(msgId);
                tTemplateData.setName(name);
                tTemplateData.setValue(value);
                tTemplateData.setColor(color);
                tTemplateData.setCreateTime(now);
                tTemplateData.setModifiedTime(now);

                templateDataMapper.insert(tTemplateData);
            }

            JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "保存成功！", "成功",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static MaSubscribeMsgForm getInstance() {
        if (maSubscribeMsgForm == null) {
            maSubscribeMsgForm = new MaSubscribeMsgForm();
        }
        return maSubscribeMsgForm;
    }

    /**
     * 初始化模板消息数据table
     */
    public static void initTemplateDataTable() {
        JTable msgDataTable = getInstance().getTemplateMsgDataTable();
        String[] headerNames = {"Name", "Value", "Color", "操作"};
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
        tableColumnModel.getColumn(3).setPreferredWidth(46);
        tableColumnModel.getColumn(3).setMaxWidth(46);
    }

    /**
     * 清空所有界面字段
     */
    public static void clearAllField() {
        getInstance().getMsgTemplateIdTextField().setText("");
        getInstance().getMsgTemplateUrlTextField().setText("");
        getInstance().getTemplateDataNameTextField().setText("");
        getInstance().getTemplateDataValueTextField().setText("");
        getInstance().getTemplateDataColorTextField().setText("");
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
        templateMsgPanel.setLayout(new GridLayoutManager(2, 1, new Insets(10, 15, 0, 0), -1, -1));
        panel1.add(templateMsgPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        templateMsgPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "小程序-订阅消息编辑", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, templateMsgPanel.getFont()), null));
        templateMsgDataPanel = new JPanel();
        templateMsgDataPanel.setLayout(new GridLayoutManager(3, 4, new Insets(10, 0, 0, 0), -1, -1));
        templateMsgPanel.add(templateMsgDataPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        templateMsgDataPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "模板变量（可使用\"${ENTER}\"作为换行符）", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, templateMsgDataPanel.getFont()), null));
        templateDataNameTextField = new JTextField();
        templateDataNameTextField.setToolTipText("当消息类型是模板消息时的示例：first或者keyword1或者remark之类的");
        templateMsgDataPanel.add(templateDataNameTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        templateDataValueTextField = new JTextField();
        templateMsgDataPanel.add(templateDataValueTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        templateDataColorTextField = new JTextField();
        templateDataColorTextField.setToolTipText("示例值：FF0000");
        templateMsgDataPanel.add(templateDataColorTextField, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        templateMsgDataAddButton = new JButton();
        templateMsgDataAddButton.setIcon(new ImageIcon(getClass().getResource("/icon/add.png")));
        templateMsgDataAddButton.setText("");
        templateMsgDataPanel.add(templateMsgDataAddButton, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        templateMsgDataTable = new JTable();
        templateMsgDataTable.setAutoCreateColumnsFromModel(true);
        templateMsgDataTable.setAutoCreateRowSorter(true);
        templateMsgDataTable.setGridColor(new Color(-12236470));
        templateMsgDataTable.setRowHeight(36);
        templateMsgDataPanel.add(templateMsgDataTable, new GridConstraints(2, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        templateMsgNameLabel = new JLabel();
        templateMsgNameLabel.setText("name");
        templateMsgNameLabel.setToolTipText("当消息类型是模板消息时的示例：first或者keyword1或者remark之类的");
        templateMsgDataPanel.add(templateMsgNameLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        templateMsgValueLabel = new JLabel();
        templateMsgValueLabel.setText("value");
        templateMsgDataPanel.add(templateMsgValueLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        templateMsgColorLabel = new JLabel();
        templateMsgColorLabel.setText("color");
        templateMsgColorLabel.setToolTipText("示例值：FF0000");
        templateMsgDataPanel.add(templateMsgColorLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 5, 0), -1, -1));
        templateMsgPanel.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        templateIdLabel = new JLabel();
        templateIdLabel.setText("模板ID *");
        panel2.add(templateIdLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgTemplateIdTextField = new JTextField();
        panel2.add(msgTemplateIdTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        templateUrlLabel = new JLabel();
        templateUrlLabel.setText("跳转页面");
        panel2.add(templateUrlLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgTemplateUrlTextField = new JTextField();
        panel2.add(msgTemplateUrlTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        templateMsgNameLabel.setLabelFor(templateDataNameTextField);
        templateMsgValueLabel.setLabelFor(templateDataValueTextField);
        templateMsgColorLabel.setLabelFor(templateDataColorTextField);
        templateIdLabel.setLabelFor(msgTemplateIdTextField);
        templateUrlLabel.setLabelFor(msgTemplateUrlTextField);
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
