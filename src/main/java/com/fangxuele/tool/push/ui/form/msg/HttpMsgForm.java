package com.fangxuele.tool.push.ui.form.msg;

import com.fangxuele.tool.push.ui.component.TableInCellButtonColumn;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * <pre>
 * Http Msg Form
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/7/14.
 */
@Getter
public class HttpMsgForm {
    private JComboBox methodComboBox;
    private JTextField urlTextField;
    private JTextField paramNameTextField;
    private JTextField paramValueTextField;
    private JButton paramAddButton;
    private JPanel httpPanel;
    private JTable paramTable;
    private JButton headerAddButton;
    private JTable headerTable;
    private JTextField headerNameTextField;
    private JTextField headerValueTextField5;
    private JButton cookieAddButton;
    private JTable cookieTable;
    private JTextField cookieNameTextField;
    private JTextField cookieValueTextField;
    private JTextField cookieDomainTextField;
    private JTextField cookiePathTextField;
    private JTextField cookieExpiryTextField;
    private JTextArea bodyTextArea;

    public static HttpMsgForm httpMsgForm = new HttpMsgForm();

    public HttpMsgForm() {
        paramAddButton.addActionListener(e -> {
            String[] data = new String[2];
            data[0] = httpMsgForm.getParamNameTextField().getText();
            data[1] = httpMsgForm.getParamValueTextField().getText();

            if (httpMsgForm.getParamTable().getModel().getRowCount() == 0) {
                initParamTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) httpMsgForm.getParamTable().getModel();
            int rowCount = tableModel.getRowCount();

            Set<String> keySet = new HashSet<>();
            String keyData;
            for (int i = 0; i < rowCount; i++) {
                keyData = (String) tableModel.getValueAt(i, 0);
                keySet.add(keyData);
            }

            if (StringUtils.isEmpty(data[0]) || StringUtils.isEmpty(data[1])) {
                JOptionPane.showMessageDialog(httpMsgForm.getHttpPanel(), "Name和Value不能为空！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (keySet.contains(data[0])) {
                JOptionPane.showMessageDialog(httpMsgForm.getHttpPanel(), "Name不能重复！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                tableModel.addRow(data);
            }
        });

        headerAddButton.addActionListener(e -> {
            String[] data = new String[2];
            data[0] = httpMsgForm.getHeaderNameTextField().getText();
            data[1] = httpMsgForm.getHeaderValueTextField5().getText();

            if (httpMsgForm.getHeaderTable().getModel().getRowCount() == 0) {
                initHeaderTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) httpMsgForm.getHeaderTable().getModel();
            int rowCount = tableModel.getRowCount();

            Set<String> keySet = new HashSet<>();
            String keyData;
            for (int i = 0; i < rowCount; i++) {
                keyData = (String) tableModel.getValueAt(i, 0);
                keySet.add(keyData);
            }

            if (StringUtils.isEmpty(data[0]) || StringUtils.isEmpty(data[1])) {
                JOptionPane.showMessageDialog(httpMsgForm.getHttpPanel(), "Name和Value不能为空！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (keySet.contains(data[0])) {
                JOptionPane.showMessageDialog(httpMsgForm.getHttpPanel(), "Name不能重复！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                tableModel.addRow(data);
            }
        });

        cookieAddButton.addActionListener(e -> {
            String[] data = new String[5];
            data[0] = httpMsgForm.getCookieNameTextField().getText();
            data[1] = httpMsgForm.getCookieValueTextField().getText();
            data[2] = httpMsgForm.getCookieDomainTextField().getText();
            data[3] = httpMsgForm.getCookiePathTextField().getText();
            data[4] = httpMsgForm.getCookieExpiryTextField().getText();

            if (httpMsgForm.getCookieTable().getModel().getRowCount() == 0) {
                initCookieTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) httpMsgForm.getCookieTable().getModel();
            int rowCount = tableModel.getRowCount();

            Set<String> keySet = new HashSet<>();
            String keyData;
            for (int i = 0; i < rowCount; i++) {
                keyData = (String) tableModel.getValueAt(i, 0);
                keySet.add(keyData);
            }

            if (StringUtils.isEmpty(data[0]) || StringUtils.isEmpty(data[1]) || StringUtils.isEmpty(data[4])) {
                JOptionPane.showMessageDialog(httpMsgForm.getHttpPanel(), "Name、Value、Expiry不能为空！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (keySet.contains(data[0])) {
                JOptionPane.showMessageDialog(httpMsgForm.getHttpPanel(), "Name不能重复！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                tableModel.addRow(data);
            }
        });
    }

    public static void init(String msgName) {
    }

    /**
     * 初始化ParamTable
     */
    public static void initParamTable() {
        JTable paramTable = httpMsgForm.getParamTable();
        paramTable.setRowHeight(36);
        String[] headerNames = {"Name", "Value", ""};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        paramTable.setModel(model);
        paramTable.updateUI();
        DefaultTableCellRenderer hr = (DefaultTableCellRenderer) paramTable.getTableHeader().getDefaultRenderer();
        // 表头列名居左
        hr.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);

        TableColumnModel tableColumnModel = paramTable.getColumnModel();
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(paramTable, headerNames.length - 1));
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(paramTable, headerNames.length - 1));

        // 设置列宽
        tableColumnModel.getColumn(headerNames.length - 1).setPreferredWidth(46);
        tableColumnModel.getColumn(headerNames.length - 1).setMaxWidth(46);
    }

    /**
     * 初始化HeaderTable
     */
    public static void initHeaderTable() {
        JTable paramTable = httpMsgForm.getHeaderTable();
        paramTable.setRowHeight(36);
        String[] headerNames = {"Name", "Value", ""};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        paramTable.setModel(model);
        paramTable.updateUI();
        DefaultTableCellRenderer hr = (DefaultTableCellRenderer) paramTable.getTableHeader().getDefaultRenderer();
        // 表头列名居左
        hr.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);

        TableColumnModel tableColumnModel = paramTable.getColumnModel();
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(paramTable, headerNames.length - 1));
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(paramTable, headerNames.length - 1));

        // 设置列宽
        tableColumnModel.getColumn(headerNames.length - 1).setPreferredWidth(46);
        tableColumnModel.getColumn(headerNames.length - 1).setMaxWidth(46);
    }

    /**
     * 初始化CookieTable
     */
    public static void initCookieTable() {
        JTable paramTable = httpMsgForm.getCookieTable();
        paramTable.setRowHeight(36);
        String[] headerNames = {"Name", "Value", "Domain", "Path", "Expiry", ""};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        paramTable.setModel(model);
        paramTable.updateUI();
        DefaultTableCellRenderer hr = (DefaultTableCellRenderer) paramTable.getTableHeader().getDefaultRenderer();
        // 表头列名居左
        hr.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);

        TableColumnModel tableColumnModel = paramTable.getColumnModel();
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(paramTable, headerNames.length - 1));
        tableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(paramTable, headerNames.length - 1));

        // 设置列宽
        tableColumnModel.getColumn(headerNames.length - 1).setPreferredWidth(46);
        tableColumnModel.getColumn(headerNames.length - 1).setMaxWidth(46);
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
        httpPanel = new JPanel();
        httpPanel.setLayout(new GridLayoutManager(5, 1, new Insets(10, 8, 0, 8), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        httpPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("URL");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        methodComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("GET");
        defaultComboBoxModel1.addElement("POST");
        defaultComboBoxModel1.addElement("PUT");
        defaultComboBoxModel1.addElement("PATCH");
        defaultComboBoxModel1.addElement("DELETE");
        defaultComboBoxModel1.addElement("HEAD");
        defaultComboBoxModel1.addElement("OPTIONS");
        methodComboBox.setModel(defaultComboBoxModel1);
        panel1.add(methodComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        urlTextField = new JTextField();
        panel1.add(urlTextField, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 5, new Insets(5, 5, 0, 0), -1, -1));
        httpPanel.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Params"));
        final JLabel label2 = new JLabel();
        label2.setText("Name");
        panel2.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        paramNameTextField = new JTextField();
        panel2.add(paramNameTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Value");
        panel2.add(label3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        paramValueTextField = new JTextField();
        panel2.add(paramValueTextField, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        paramAddButton = new JButton();
        paramAddButton.setIcon(new ImageIcon(getClass().getResource("/icon/add.png")));
        paramAddButton.setText("");
        panel2.add(paramAddButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        paramTable = new JTable();
        paramTable.setRowHeight(36);
        panel2.add(paramTable, new GridConstraints(1, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 5, new Insets(5, 5, 0, 0), -1, -1));
        httpPanel.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Headers"));
        final JLabel label4 = new JLabel();
        label4.setText("Name");
        panel3.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        headerNameTextField = new JTextField();
        panel3.add(headerNameTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Value");
        panel3.add(label5, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        headerValueTextField5 = new JTextField();
        panel3.add(headerValueTextField5, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        headerAddButton = new JButton();
        headerAddButton.setIcon(new ImageIcon(getClass().getResource("/icon/add.png")));
        headerAddButton.setText("");
        panel3.add(headerAddButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        headerTable = new JTable();
        headerTable.setRowHeight(36);
        panel3.add(headerTable, new GridConstraints(1, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(3, 6, new Insets(5, 5, 0, 0), -1, -1));
        httpPanel.add(panel4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Cookies"));
        cookieNameTextField = new JTextField();
        panel4.add(cookieNameTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        cookieValueTextField = new JTextField();
        panel4.add(cookieValueTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        cookieAddButton = new JButton();
        cookieAddButton.setIcon(new ImageIcon(getClass().getResource("/icon/add.png")));
        cookieAddButton.setText("");
        panel4.add(cookieAddButton, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cookieTable = new JTable();
        cookieTable.setRowHeight(36);
        panel4.add(cookieTable, new GridConstraints(2, 0, 1, 6, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        cookieDomainTextField = new JTextField();
        panel4.add(cookieDomainTextField, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        cookiePathTextField = new JTextField();
        panel4.add(cookiePathTextField, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Name");
        panel4.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Value");
        panel4.add(label7, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final JLabel label8 = new JLabel();
        label8.setText("Domain");
        panel4.add(label8, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Path");
        panel4.add(label9, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("Expiry DateTime");
        panel4.add(label10, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cookieExpiryTextField = new JTextField();
        panel4.add(cookieExpiryTextField, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(5, 0, 0, 0), -1, -1));
        httpPanel.add(panel5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Body"));
        bodyTextArea = new JTextArea();
        panel5.add(bodyTextArea, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return httpPanel;
    }

}
