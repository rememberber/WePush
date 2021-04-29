package com.fangxuele.tool.push.ui.form;

import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TPeopleDataMapper;
import com.fangxuele.tool.push.dao.TPeopleImportConfigMapper;
import com.fangxuele.tool.push.dao.TPeopleMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TPeople;
import com.fangxuele.tool.push.domain.TPeopleData;
import com.fangxuele.tool.push.domain.TPeopleImportConfig;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.PeopleImportWayEnum;
import com.fangxuele.tool.push.util.JTableUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.UndoUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;

/**
 * PeopleEditForm
 */
@Getter
public class PeopleEditForm {
    private JPanel mainPanel;
    private JTextField searchTextField;
    private JButton searchButton;
    private JTable memberListTable;
    private JButton selectAllButton;
    private JButton importButton;
    private JButton exportButton;
    private JButton deleteButton;
    private JPanel memberTabUpPanel;
    private JProgressBar memberTabImportProgressBar;
    private JLabel memberTabCountLabel;
    private JLabel peopleNameLabel;
    private JLabel peopleAccountLabel;
    private JLabel peopleMsgTypeLabel;
    private JLabel lastImportWayLabel;

    private static PeopleEditForm peopleEditForm;

    private static TPeopleMapper peopleMapper = MybatisUtil.getSqlSession().getMapper(TPeopleMapper.class);
    private static TPeopleDataMapper peopleDataMapper = MybatisUtil.getSqlSession().getMapper(TPeopleDataMapper.class);
    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TPeopleImportConfigMapper peopleImportConfigMapper = MybatisUtil.getSqlSession().getMapper(TPeopleImportConfigMapper.class);

    private PeopleEditForm() {
        UndoUtil.register(this);
    }

    public static PeopleEditForm getInstance() {
        if (peopleEditForm == null) {
            peopleEditForm = new PeopleEditForm();
        }
        return peopleEditForm;
    }

    /**
     * 初始化tab
     */
    public static void init(String selectedPeopleName) {
        peopleEditForm = getInstance();
        peopleEditForm.getMemberTabImportProgressBar().setVisible(false);
        // 设置滚动条速度
//        peopleEditForm.getAccountEditScrollPane().getVerticalScrollBar().setUnitIncrement(15);
//        peopleEditForm.getAccountEditScrollPane().getVerticalScrollBar().setDoubleBuffered(true);

    }

    /**
     * 初始化人群数据列表
     *
     * @param peopleId
     */
    public static void initDataTable(int peopleId) {

        // -----init Info

        TPeople tPeople = peopleMapper.selectByPrimaryKey(peopleId);
        if (tPeople != null) {
            // peopleName
            String peopleName = tPeople.getPeopleName();
            peopleEditForm.getPeopleNameLabel().setText(peopleName);

            // count
            Long totalCount = peopleDataMapper.countByPeopleId(peopleId);
            if (totalCount != null) {
                peopleEditForm.getMemberTabCountLabel().setText(String.valueOf(totalCount));
            }

            // account
            Integer accountId = tPeople.getAccountId();
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            if (tAccount != null) {
                peopleEditForm.getPeopleAccountLabel().setText(tAccount.getAccountName());
            }

            // msgType
            String msgType = tPeople.getMsgType();
            String msgTypeName = MessageTypeEnum.getName(Integer.parseInt(msgType));
            peopleEditForm.getPeopleMsgTypeLabel().setText(msgTypeName);

            // 上一次导入方式
            TPeopleImportConfig tPeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);
            if (tPeopleImportConfig != null) {
                peopleEditForm.getLastImportWayLabel().setText(PeopleImportWayEnum.getName(Integer.parseInt(tPeopleImportConfig.getLastWay())));
            }
        }

        // -----init Table

        JTable memberListTable = peopleEditForm.getMemberListTable();

        // 人群数据列表
        String[] headerNames = {"PIN", "VarData", "id"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        memberListTable.setModel(model);
        // 隐藏表头
//        JTableUtil.hideTableHeader(memberListTable);

        Object[] data;

        List<TPeopleData> peopleDataList = peopleDataMapper.selectByPeopleId(peopleId);
        for (TPeopleData peopleData : peopleDataList) {
            data = new Object[3];
            data[0] = peopleData.getPin();
            data[1] = peopleData.getVarData();
            data[2] = peopleData.getId();
            model.addRow(data);
        }
        // 隐藏id列
        JTableUtil.hideColumn(memberListTable, 2);
        // 设置列宽
        TableColumnModel tableColumnModel = memberListTable.getColumnModel();
        tableColumnModel.getColumn(0).setPreferredWidth(peopleEditForm.getImportButton().getWidth() * 3);
        tableColumnModel.getColumn(0).setMaxWidth(peopleEditForm.getImportButton().getWidth() * 3);
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(4, 1, new Insets(10, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(5, 0, 0, 5), -1, -1));
        mainPanel.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        searchTextField = new JTextField();
        panel1.add(searchTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        searchButton = new JButton();
        searchButton.setIcon(new ImageIcon(getClass().getResource("/icon/find_dark.png")));
        searchButton.setText("搜索");
        panel1.add(searchButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        mainPanel.add(scrollPane1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        memberListTable = new JTable();
        memberListTable.setGridColor(new Color(-12236470));
        memberListTable.setMinimumSize(new Dimension(30, 30));
        memberListTable.setRowHeight(36);
        scrollPane1.setViewportView(memberListTable);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 6, new Insets(0, 5, 5, 5), -1, -1));
        mainPanel.add(panel2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        selectAllButton = new JButton();
        selectAllButton.setIcon(new ImageIcon(getClass().getResource("/icon/selectall_dark.png")));
        selectAllButton.setText("清空");
        panel2.add(selectAllButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        importButton = new JButton();
        importButton.setIcon(new ImageIcon(getClass().getResource("/icon/import_dark.png")));
        importButton.setText("导入");
        panel2.add(importButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        exportButton = new JButton();
        exportButton.setIcon(new ImageIcon(getClass().getResource("/icon/export_dark.png")));
        exportButton.setText("导出");
        panel2.add(exportButton, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deleteButton = new JButton();
        deleteButton.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
        deleteButton.setText("删除");
        panel2.add(deleteButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("注：列表仅展示20条数据，查看更多请导出");
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        memberTabUpPanel = new JPanel();
        memberTabUpPanel.setLayout(new GridLayoutManager(6, 3, new Insets(0, 5, 5, 0), -1, -1));
        mainPanel.add(memberTabUpPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        memberTabImportProgressBar = new JProgressBar();
        memberTabUpPanel.add(memberTabImportProgressBar, new GridConstraints(5, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        memberTabUpPanel.add(spacer2, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("人群数量：");
        memberTabUpPanel.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("人群名称：");
        memberTabUpPanel.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        peopleNameLabel = new JLabel();
        peopleNameLabel.setText("-");
        memberTabUpPanel.add(peopleNameLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("所属账号：");
        memberTabUpPanel.add(label4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        peopleAccountLabel = new JLabel();
        peopleAccountLabel.setText("-");
        memberTabUpPanel.add(peopleAccountLabel, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        peopleMsgTypeLabel = new JLabel();
        peopleMsgTypeLabel.setText("-");
        memberTabUpPanel.add(peopleMsgTypeLabel, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lastImportWayLabel = new JLabel();
        lastImportWayLabel.setText("-");
        memberTabUpPanel.add(lastImportWayLabel, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("所属消息类型：");
        memberTabUpPanel.add(label5, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("上次导入方式：");
        memberTabUpPanel.add(label6, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        memberTabCountLabel = new JLabel();
        memberTabCountLabel.setText("-");
        memberTabUpPanel.add(memberTabCountLabel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
