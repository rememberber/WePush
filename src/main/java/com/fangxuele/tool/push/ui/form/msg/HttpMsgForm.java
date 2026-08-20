package com.fangxuele.tool.push.ui.form.msg;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgHttp;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.component.TableInCellButtonColumn;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.fangxuele.tool.push.util.UIUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
public class HttpMsgForm implements IMsgForm {
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
    private JTabbedPane tabbedPane1;
    private JComboBox bodyTypeComboBox;

    private static HttpMsgForm httpMsgForm;

    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    public HttpMsgForm() {
        paramAddButton.setIcon(new FlatSVGIcon("icon/add.svg"));
        cookieAddButton.setIcon(new FlatSVGIcon("icon/add.svg"));
        headerAddButton.setIcon(new FlatSVGIcon("icon/add.svg"));

        paramAddButton.addActionListener(e -> {
            String[] data = new String[2];
            data[0] = getInstance().getParamNameTextField().getText();
            data[1] = getInstance().getParamValueTextField().getText();

            if (getInstance().getParamTable().getModel().getRowCount() == 0) {
                initParamTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) getInstance().getParamTable().getModel();
            int rowCount = tableModel.getRowCount();

            Set<String> keySet = new HashSet<>();
            String keyData;
            for (int i = 0; i < rowCount; i++) {
                keyData = (String) tableModel.getValueAt(i, 0);
                keySet.add(keyData);
            }

            if (StringUtils.isEmpty(data[0]) || StringUtils.isEmpty(data[1])) {
                JOptionPane.showMessageDialog(getInstance().getHttpPanel(), "Name和Value不能为空！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (keySet.contains(data[0])) {
                JOptionPane.showMessageDialog(getInstance().getHttpPanel(), "Name不能重复！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                tableModel.addRow(data);
            }
        });

        headerAddButton.addActionListener(e -> {
            String[] data = new String[2];
            data[0] = getInstance().getHeaderNameTextField().getText();
            data[1] = getInstance().getHeaderValueTextField5().getText();

            if (getInstance().getHeaderTable().getModel().getRowCount() == 0) {
                initHeaderTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) getInstance().getHeaderTable().getModel();
            int rowCount = tableModel.getRowCount();

            Set<String> keySet = new HashSet<>();
            String keyData;
            for (int i = 0; i < rowCount; i++) {
                keyData = (String) tableModel.getValueAt(i, 0);
                keySet.add(keyData);
            }

            if (StringUtils.isEmpty(data[0]) || StringUtils.isEmpty(data[1])) {
                JOptionPane.showMessageDialog(getInstance().getHttpPanel(), "Name和Value不能为空！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (keySet.contains(data[0])) {
                JOptionPane.showMessageDialog(getInstance().getHttpPanel(), "Name不能重复！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                tableModel.addRow(data);
            }
        });

        cookieAddButton.addActionListener(e -> {
            String[] data = new String[5];
            data[0] = getInstance().getCookieNameTextField().getText();
            data[1] = getInstance().getCookieValueTextField().getText();
            data[2] = getInstance().getCookieDomainTextField().getText();
            data[3] = getInstance().getCookiePathTextField().getText();
            data[4] = getInstance().getCookieExpiryTextField().getText();

            if (getInstance().getCookieTable().getModel().getRowCount() == 0) {
                initCookieTable();
            }

            DefaultTableModel tableModel = (DefaultTableModel) getInstance().getCookieTable().getModel();
            int rowCount = tableModel.getRowCount();

            Set<String> keySet = new HashSet<>();
            String keyData;
            for (int i = 0; i < rowCount; i++) {
                keyData = (String) tableModel.getValueAt(i, 0);
                keySet.add(keyData);
            }

            if (StringUtils.isEmpty(data[0]) || StringUtils.isEmpty(data[1]) || StringUtils.isEmpty(data[4])) {
                JOptionPane.showMessageDialog(getInstance().getHttpPanel(), "Name、Value、Expiry不能为空！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else if (keySet.contains(data[0])) {
                JOptionPane.showMessageDialog(getInstance().getHttpPanel(), "Name不能重复！", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                tableModel.addRow(data);
            }
        });

        // 消息类型切换事件
        methodComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                switchMethod(e.getItem().toString());
            }
        });

        if (UIUtil.isDarkLaf()) {
            Color bgColor = new Color(43, 43, 43);
            bodyTextArea.setBackground(bgColor);
            Color foreColor = new Color(187, 187, 187);
            bodyTextArea.setForeground(foreColor);
        }
    }

    @Override
    public void init(Integer msgId) {
        clearAllField();

        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);

        if (tMsg == null) {
            return;
        }

        TMsgHttp tMsgHttp = JSONUtil.toBean(tMsg.getContent(), TMsgHttp.class);

        getInstance().getMethodComboBox().setSelectedItem(tMsgHttp.getMethod());
        getInstance().getUrlTextField().setText(tMsgHttp.getUrl());
        getInstance().getBodyTextArea().setText(tMsgHttp.getBody());
        getInstance().getBodyTypeComboBox().setSelectedItem(tMsgHttp.getBodyType());
        switchMethod(tMsgHttp.getMethod());

        MessageEditForm messageEditForm = MessageEditForm.getInstance();
        messageEditForm.getMsgNameField().setText(tMsg.getMsgName());
        messageEditForm.getPreviewUserField().setText(tMsg.getPreviewUser());

        // Params=====================================
        initParamTable();
        List<NameValueObject> params = JSONUtil.toList(JSONUtil.parseArray(tMsgHttp.getParams()), NameValueObject.class);
        String[] headerNames = {"Name", "Value", ""};
        Object[][] cellData = new String[params.size()][headerNames.length];
        for (int i = 0; i < params.size(); i++) {
            NameValueObject nameValueObject = params.get(i);
            cellData[i][0] = nameValueObject.getName();
            cellData[i][1] = nameValueObject.getValue();
        }
        DefaultTableModel model = new DefaultTableModel(cellData, headerNames);
        getInstance().getParamTable().setModel(model);
        TableColumnModel paramTableColumnModel = getInstance().getParamTable().getColumnModel();
        paramTableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(getInstance().getParamTable(), headerNames.length - 1));
        paramTableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(getInstance().getParamTable(), headerNames.length - 1));

        // 设置列宽
        paramTableColumnModel.getColumn(2).setPreferredWidth(46);
        paramTableColumnModel.getColumn(2).setMaxWidth(46);
        // Headers=====================================
        initHeaderTable();
        List<NameValueObject> headers = JSONUtil.toList(JSONUtil.parseArray(tMsgHttp.getHeaders()), NameValueObject.class);
        cellData = new String[headers.size()][headerNames.length];
        for (int i = 0; i < headers.size(); i++) {
            NameValueObject nameValueObject = headers.get(i);
            cellData[i][0] = nameValueObject.getName();
            cellData[i][1] = nameValueObject.getValue();
        }
        model = new DefaultTableModel(cellData, headerNames);
        getInstance().getHeaderTable().setModel(model);
        TableColumnModel headerTableColumnModel = getInstance().getHeaderTable().getColumnModel();
        headerTableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(getInstance().getHeaderTable(), headerNames.length - 1));
        headerTableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(getInstance().getHeaderTable(), headerNames.length - 1));

        // 设置列宽
        headerTableColumnModel.getColumn(2).setPreferredWidth(46);
        headerTableColumnModel.getColumn(2).setMaxWidth(46);
        // Cookies=====================================
        initCookieTable();
        List<CookieObject> cookies = JSONUtil.toList(JSONUtil.parseArray(tMsgHttp.getCookies()), CookieObject.class);
        headerNames = new String[]{"Name", "Value", "Domain", "Path", "Expiry", ""};
        cellData = new String[cookies.size()][headerNames.length];
        for (int i = 0; i < cookies.size(); i++) {
            CookieObject cookieObject = cookies.get(i);
            cellData[i][0] = cookieObject.getName();
            cellData[i][1] = cookieObject.getValue();
            cellData[i][2] = cookieObject.getDomain();
            cellData[i][3] = cookieObject.getPath();
            cellData[i][4] = cookieObject.getExpiry();
        }
        model = new DefaultTableModel(cellData, headerNames);
        getInstance().getCookieTable().setModel(model);
        TableColumnModel cookieTableColumnModel = getInstance().getCookieTable().getColumnModel();
        cookieTableColumnModel.getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(getInstance().getCookieTable(), headerNames.length - 1));
        cookieTableColumnModel.getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(getInstance().getCookieTable(), headerNames.length - 1));

        // 设置列宽
        cookieTableColumnModel.getColumn(5).setPreferredWidth(46);
        cookieTableColumnModel.getColumn(5).setMaxWidth(46);
    }

    @Override
    public void save(Integer accountId, String msgName) {
        boolean existSameMsg = false;
        Integer msgId = null;
        TMsg tMsg = msgMapper.selectByUnique(MessageTypeEnum.HTTP_CODE, accountId, msgName);
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
            String method = (String) getInstance().getMethodComboBox().getSelectedItem();
            String url = getInstance().getUrlTextField().getText();
            String body = getInstance().getBodyTextArea().getText();
            String bodyType = (String) getInstance().getBodyTypeComboBox().getSelectedItem();
            String now = SqliteUtil.nowDateForSqlite();

            TMsg msg = new TMsg();
            TMsgHttp tMsgHttp = new TMsgHttp();
            msg.setMsgType(MessageTypeEnum.HTTP_CODE);
            msg.setAccountId(accountId);
            msg.setMsgName(msgName);
            tMsgHttp.setMethod(method);
            tMsgHttp.setUrl(url);
            tMsgHttp.setBody(body);
            tMsgHttp.setBodyType(bodyType);
            msg.setCreateTime(now);
            msg.setModifiedTime(now);
            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            msg.setPreviewUser(messageEditForm.getPreviewUserField().getText());

            // =============params
            // 如果table为空，则初始化
            if (getInstance().getParamTable().getModel().getRowCount() == 0) {
                initParamTable();
            }
            // 逐行读取
            DefaultTableModel paraTableModel = (DefaultTableModel) getInstance().getParamTable().getModel();
            int rowCount = paraTableModel.getRowCount();
            List<NameValueObject> params = new ArrayList<>();
            NameValueObject nameValueObject;
            for (int i = 0; i < rowCount; i++) {
                String name = (String) paraTableModel.getValueAt(i, 0);
                String value = (String) paraTableModel.getValueAt(i, 1);
                nameValueObject = new NameValueObject();
                nameValueObject.setName(name);
                nameValueObject.setValue(value);
                params.add(nameValueObject);
            }
            tMsgHttp.setParams(JSONUtil.toJsonStr(params));
            // =============headers
            // 如果table为空，则初始化
            if (getInstance().getHeaderTable().getModel().getRowCount() == 0) {
                initHeaderTable();
            }
            // 逐行读取
            DefaultTableModel headerTableModel = (DefaultTableModel) getInstance().getHeaderTable().getModel();
            rowCount = headerTableModel.getRowCount();
            List<NameValueObject> headers = new ArrayList<>();
            for (int i = 0; i < rowCount; i++) {
                String name = (String) headerTableModel.getValueAt(i, 0);
                String value = (String) headerTableModel.getValueAt(i, 1);
                nameValueObject = new NameValueObject();
                nameValueObject.setName(name);
                nameValueObject.setValue(value);
                headers.add(nameValueObject);
            }
            tMsgHttp.setHeaders(JSONUtil.toJsonStr(headers));
            // =============cookies
            // 如果table为空，则初始化
            if (getInstance().getCookieTable().getModel().getRowCount() == 0) {
                initCookieTable();
            }
            // 逐行读取
            DefaultTableModel cookiesTableModel = (DefaultTableModel) getInstance().getCookieTable().getModel();
            rowCount = cookiesTableModel.getRowCount();
            List<CookieObject> cookies = new ArrayList<>();
            CookieObject cookieObject;
            for (int i = 0; i < rowCount; i++) {
                String name = (String) cookiesTableModel.getValueAt(i, 0);
                String value = (String) cookiesTableModel.getValueAt(i, 1);
                String domain = (String) cookiesTableModel.getValueAt(i, 2);
                String path = (String) cookiesTableModel.getValueAt(i, 3);
                String expiry = (String) cookiesTableModel.getValueAt(i, 4);
                cookieObject = new CookieObject();
                cookieObject.setName(name);
                cookieObject.setValue(value);
                cookieObject.setDomain(domain);
                cookieObject.setPath(path);
                cookieObject.setExpiry(expiry);
                cookies.add(cookieObject);
            }
            tMsgHttp.setCookies(JSONUtil.toJsonStr(cookies));

            msg.setContent(JSONUtil.toJsonStr(tMsgHttp));
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

    public static HttpMsgForm getInstance() {
        if (httpMsgForm == null) {
            httpMsgForm = new HttpMsgForm();
            switchMethod("GET");
        }
        return httpMsgForm;
    }

    /**
     * 清空所有界面字段
     */
    @Override
    public void clearAllField() {
        getInstance().getMethodComboBox().setSelectedIndex(0);
        getInstance().getUrlTextField().setText("");
        getInstance().getParamNameTextField().setText("");
        getInstance().getParamValueTextField().setText("");
        getInstance().getHeaderNameTextField().setText("");
        getInstance().getHeaderValueTextField5().setText("");
        getInstance().getCookieNameTextField().setText("");
        getInstance().getCookieValueTextField().setText("");
        getInstance().getCookieDomainTextField().setText("");
        getInstance().getCookiePathTextField().setText("");
        getInstance().getCookieExpiryTextField().setText("");
        getInstance().getBodyTextArea().setText("");
        getInstance().getBodyTypeComboBox().setSelectedIndex(0);
        initParamTable();
        initHeaderTable();
        initCookieTable();
    }

    /**
     * 切换方法
     *
     * @param method
     */
    private static void switchMethod(String method) {
        if ("GET".equals(method)) {
            getInstance().getBodyTextArea().setText("");
            getInstance().getTabbedPane1().setEnabledAt(3, false);
        } else {
            getInstance().getTabbedPane1().setEnabledAt(3, true);
        }
    }

    /**
     * 初始化ParamTable
     */
    public static void initParamTable() {
        JTable paramTable = getInstance().getParamTable();
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
        JTable paramTable = getInstance().getHeaderTable();
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
        JTable paramTable = getInstance().getCookieTable();
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

    @Getter
    @Setter
    public static class NameValueObject {

        private String name;

        private String value;
    }

    @Getter
    @Setter
    public static class CookieObject {

        private String name;

        private String value;

        private String domain;

        private String path;

        private String expiry;
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
        httpPanel.setLayout(new GridLayoutManager(2, 1, new Insets(10, 8, 0, 8), -1, -1));
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
        tabbedPane1 = new JTabbedPane();
        httpPanel.add(tabbedPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Params", panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 3, new Insets(5, 5, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        paramNameTextField = new JTextField();
        panel3.add(paramNameTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        paramValueTextField = new JTextField();
        panel3.add(paramValueTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        paramAddButton = new JButton();
        paramAddButton.setText("");
        panel3.add(paramAddButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        paramTable = new JTable();
        paramTable.setRowHeight(36);
        panel3.add(paramTable, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Name");
        panel3.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Value");
        panel3.add(label3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Headers", panel4);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(3, 3, new Insets(5, 5, 0, 0), -1, -1));
        panel4.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        headerNameTextField = new JTextField();
        panel5.add(headerNameTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        headerValueTextField5 = new JTextField();
        panel5.add(headerValueTextField5, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        headerAddButton = new JButton();
        headerAddButton.setText("");
        panel5.add(headerAddButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        headerTable = new JTable();
        headerTable.setRowHeight(36);
        panel5.add(headerTable, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Name");
        panel5.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Value");
        panel5.add(label5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Cookies", panel6);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(3, 6, new Insets(5, 5, 0, 0), -1, -1));
        panel6.add(panel7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        cookieNameTextField = new JTextField();
        panel7.add(cookieNameTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        cookieValueTextField = new JTextField();
        panel7.add(cookieValueTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        cookieAddButton = new JButton();
        cookieAddButton.setText("");
        panel7.add(cookieAddButton, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cookieTable = new JTable();
        cookieTable.setRowHeight(36);
        panel7.add(cookieTable, new GridConstraints(2, 0, 1, 6, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        cookieDomainTextField = new JTextField();
        panel7.add(cookieDomainTextField, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        cookiePathTextField = new JTextField();
        panel7.add(cookiePathTextField, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Name");
        panel7.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Value");
        panel7.add(label7, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final JLabel label8 = new JLabel();
        label8.setText("Domain");
        panel7.add(label8, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Path");
        panel7.add(label9, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("Expiry DateTime");
        panel7.add(label10, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cookieExpiryTextField = new JTextField();
        panel7.add(cookieExpiryTextField, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Body", panel8);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(2, 2, new Insets(5, 0, 0, 0), -1, -1));
        panel8.add(panel9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        bodyTextArea = new JTextArea();
        panel9.add(bodyTextArea, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        bodyTypeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("text/plain");
        defaultComboBoxModel2.addElement("application/json");
        defaultComboBoxModel2.addElement("application/javascript");
        defaultComboBoxModel2.addElement("application/xml");
        defaultComboBoxModel2.addElement("text/xml");
        defaultComboBoxModel2.addElement("text/html");
        bodyTypeComboBox.setModel(defaultComboBoxModel2);
        panel9.add(bodyTypeComboBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel9.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return httpPanel;
    }

}
