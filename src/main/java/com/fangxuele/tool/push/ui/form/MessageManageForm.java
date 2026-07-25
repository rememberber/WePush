package com.fangxuele.tool.push.ui.form;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.ui.form.msg.MsgFormFactory;
import com.fangxuele.tool.push.util.JTableUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.UiThreadUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.google.common.collect.Maps;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
    private JButton msgHisTableDeleteButton;
    private JButton createMsgButton;
    private JComboBox accountComboBox;
    private JPanel panel1;

    private static MessageManageForm messageManageForm;

    public static Map<String, Integer> accountMap;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);

    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    public static boolean accountSwitchComboBoxListenIgnore = false;

    private static final AtomicInteger accountComboLoadSeq = new AtomicInteger(0);

    private static final AtomicInteger messageListLoadSeq = new AtomicInteger(0);

    private MessageManageForm() {
    }

    public static MessageManageForm getInstance() {
        if (messageManageForm == null) {
            messageManageForm = new MessageManageForm();
        }
        return messageManageForm;
    }

    /**
     * 初始化消息列表
     */
    public static void init() {
        messageManageForm = getInstance();

        messageManageForm.getCreateMsgButton().setIcon(new FlatSVGIcon("icon/add.svg"));
        messageManageForm.getMsgHisTableDeleteButton().setIcon(new FlatSVGIcon("icon/remove.svg"));

        MessageEditForm.getInstance().getMsgNameField().setText("");
        MsgFormFactory.getMsgForm().clearAllField();

        initAccountComboBox();
    }

    private static void initAccountComboBox() {
        int msgType = App.config.getMsgType();
        final int loadSeq = accountComboLoadSeq.incrementAndGet();
        UiThreadUtil.runOffUi(() -> {
            List<TAccount> tAccountList = accountMapper.selectByMsgType(msgType);
            UiThreadUtil.runOnUi(() -> {
                if (loadSeq != accountComboLoadSeq.get()) {
                    return;
                }
                accountMap = Maps.newHashMap();
                accountSwitchComboBoxListenIgnore = true;
                messageManageForm.getAccountComboBox().removeAllItems();
                for (TAccount tAccount : tAccountList) {
                    String accountName = tAccount.getAccountName();
                    messageManageForm.getAccountComboBox().addItem(accountName);
                    accountMap.put(accountName, tAccount.getId());
                }
                accountSwitchComboBoxListenIgnore = false;
                initMessageList();
            });
        });
    }

    public static void initMessageList() {
        int msgType = App.config.getMsgType();
        String selectedAccountName = (String) messageManageForm.getAccountComboBox().getSelectedItem();
        Integer selectedAccountId = accountMap == null ? null : accountMap.get(selectedAccountName);
        final int loadSeq = messageListLoadSeq.incrementAndGet();

        UiThreadUtil.runOffUi(() -> {
            List<TMsg> tMsgList = msgMapper.selectByMsgTypeAndAccountId(msgType, selectedAccountId);
            List<Object[]> rows = new ArrayList<>(tMsgList.size());
            for (TMsg tMsg : tMsgList) {
                rows.add(new Object[]{tMsg.getMsgName(), tMsg.getId()});
            }
            UiThreadUtil.runOnUi(() -> {
                if (loadSeq != messageListLoadSeq.get()) {
                    return;
                }
                String[] headerNames = {"消息名称", "ID"};
                DefaultTableModel model = new DefaultTableModel(null, headerNames);
                messageManageForm.getMsgHistable().setModel(model);
                JTableUtil.hideTableHeader(messageManageForm.getMsgHistable());
                for (Object[] row : rows) {
                    model.addRow(row);
                }
                JTableUtil.hideColumn(messageManageForm.getMsgHistable(), 1);
            });
        });
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
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        messageManagePanel = new JPanel();
        messageManagePanel.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 0), -1, -1));
        messageManagePanel.setMaximumSize(new Dimension(-1, -1));
        messageManagePanel.setMinimumSize(new Dimension(-1, -1));
        messageManagePanel.setPreferredSize(new Dimension(280, -1));
        panel2.add(messageManagePanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        messageManagePanel.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        msgHistable = new JTable();
        msgHistable.setGridColor(new Color(-12236470));
        msgHistable.setRowHeight(36);
        msgHistable.setShowVerticalLines(false);
        scrollPane1.setViewportView(msgHistable);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        messageManagePanel.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        msgHisTableDeleteButton = new JButton();
        msgHisTableDeleteButton.setText("删除");
        panel3.add(msgHisTableDeleteButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        createMsgButton = new JButton();
        createMsgButton.setEnabled(true);
        createMsgButton.setText("新建");
        panel3.add(createMsgButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        messageManagePanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        accountComboBox = new JComboBox();
        panel1.add(accountComboBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }
}
