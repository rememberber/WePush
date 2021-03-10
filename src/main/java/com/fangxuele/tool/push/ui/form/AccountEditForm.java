package com.fangxuele.tool.push.ui.form;

import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.form.msg.*;
import com.fangxuele.tool.push.util.UndoUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.Locale;

/**
 * <pre>
 * MessageEditForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/5/6.
 */
@Getter
public class AccountEditForm {
    private JPanel accountEditPanel;
    private JLabel accountNameLabel;
    private JTextField accountNameField;
    private JButton accountSaveButton;
    private JPanel accountEditorPanel;
    private JScrollPane accountEditScrollPane;

    private static AccountEditForm messageEditForm;

    private AccountEditForm() {
        UndoUtil.register(this);
    }

    public static AccountEditForm getInstance() {
        if (messageEditForm == null) {
            messageEditForm = new AccountEditForm();
        }
        return messageEditForm;
    }

    /**
     * 初始化消息tab
     */
    public static void init(String selectedMsgName) {
        messageEditForm = getInstance();
        // 设置滚动条速度
        messageEditForm.getAccountEditScrollPane().getVerticalScrollBar().setUnitIncrement(15);
        messageEditForm.getAccountEditScrollPane().getVerticalScrollBar().setDoubleBuffered(true);

        MsgFormFactory.getMsgForm().init(selectedMsgName);
    }

    /**
     * 根据消息类型转换界面显示
     *
     * @param msgType
     */
    public static void switchMsgType(int msgType) {
        messageEditForm = getInstance();
        messageEditForm.getAccountEditorPanel().removeAll();
        GridConstraints gridConstraintsRow0 = new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false);
        GridConstraints gridConstraintsRow1 = new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false);

        MsgFormFactory.getMsgForm().init(null);
        switch (msgType) {
            case MessageTypeEnum.MP_TEMPLATE_CODE:
                messageEditForm.getAccountEditorPanel().add(MpTemplateMsgForm.getInstance().getTemplateMsgPanel(), gridConstraintsRow0);
                break;
            case MessageTypeEnum.MA_TEMPLATE_CODE:
            case MessageTypeEnum.MA_SUBSCRIBE_CODE:
                messageEditForm.getAccountEditorPanel().add(MaSubscribeMsgForm.getInstance().getTemplateMsgPanel(), gridConstraintsRow0);
                break;
            case MessageTypeEnum.KEFU_CODE:
                messageEditForm.getAccountEditorPanel().add(KefuMsgForm.getInstance().getKefuMsgPanel(), gridConstraintsRow0);
                break;
            case MessageTypeEnum.KEFU_PRIORITY_CODE:
                messageEditForm.getAccountEditorPanel().setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
                messageEditForm.getAccountEditorPanel().add(KefuMsgForm.getInstance().getKefuMsgPanel(), gridConstraintsRow0);
                messageEditForm.getAccountEditorPanel().add(MpTemplateMsgForm.getInstance().getTemplateMsgPanel(), gridConstraintsRow1);
                break;
            case MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE:
                messageEditForm.getAccountEditorPanel().setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
                messageEditForm.getAccountEditorPanel().add(MaSubscribeMsgForm.getInstance().getTemplateMsgPanel(), gridConstraintsRow0);
                messageEditForm.getAccountEditorPanel().add(MpTemplateMsgForm.getInstance().getTemplateMsgPanel(), gridConstraintsRow1);
                break;
            case MessageTypeEnum.ALI_YUN_CODE:
                messageEditForm.getAccountEditorPanel().add(AliYunMsgForm.getInstance().getTemplateMsgPanel(), gridConstraintsRow0);
                break;
            case MessageTypeEnum.TX_YUN_CODE:
                messageEditForm.getAccountEditorPanel().add(TxYunMsgForm.getInstance().getTemplateMsgPanel(), gridConstraintsRow0);
                break;
            case MessageTypeEnum.QI_NIU_YUN_CODE:
                messageEditForm.getAccountEditorPanel().add(QiNiuYunMsgForm.getInstance().getTemplateMsgPanel(), gridConstraintsRow0);
                break;
            case MessageTypeEnum.UP_YUN_CODE:
                messageEditForm.getAccountEditorPanel().add(UpYunMsgForm.getInstance().getTemplateMsgPanel(), gridConstraintsRow0);
                break;
            case MessageTypeEnum.HW_YUN_CODE:
                messageEditForm.getAccountEditorPanel().add(HwYunMsgForm.getInstance().getTemplateMsgPanel(), gridConstraintsRow0);
                break;
            case MessageTypeEnum.YUN_PIAN_CODE:
                messageEditForm.getAccountEditorPanel().add(YunpianMsgForm.getInstance().getYunpianMsgPanel(), gridConstraintsRow0);
                break;
            case MessageTypeEnum.EMAIL_CODE:
                messageEditForm.getAccountEditorPanel().add(MailMsgForm.getInstance().getMailPanel(), gridConstraintsRow0);
                break;
            case MessageTypeEnum.WX_CP_CODE:
                messageEditForm.getAccountEditorPanel().add(WxCpMsgForm.getInstance().getWxCpMsgPanel(), gridConstraintsRow0);
                break;
            case MessageTypeEnum.HTTP_CODE:
                messageEditForm.getAccountEditorPanel().add(HttpMsgForm.getInstance().getHttpPanel(), gridConstraintsRow0);
                break;
            case MessageTypeEnum.DING_CODE:
                messageEditForm.getAccountEditorPanel().add(DingMsgForm.getInstance().getDingMsgPanel(), gridConstraintsRow0);
                break;
            case MessageTypeEnum.BD_YUN_CODE:
                messageEditForm.getAccountEditorPanel().add(BdYunMsgForm.getInstance().getTemplateMsgPanel(), gridConstraintsRow0);
                break;
            default:
                break;
        }
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
        accountEditPanel = new JPanel();
        accountEditPanel.setLayout(new GridLayoutManager(3, 1, new Insets(6, 2, 0, 5), -1, -1));
        accountEditPanel.setMaximumSize(new Dimension(-1, -1));
        accountEditPanel.setMinimumSize(new Dimension(-1, -1));
        accountEditPanel.setPreferredSize(new Dimension(-1, -1));
        panel1.add(accountEditPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 5, 10, 5), -1, -1));
        accountEditPanel.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        accountNameLabel = new JLabel();
        Font accountNameLabelFont = this.$$$getFont$$$(null, Font.BOLD, -1, accountNameLabel.getFont());
        if (accountNameLabelFont != null) accountNameLabel.setFont(accountNameLabelFont);
        accountNameLabel.setText("账号名称 *");
        accountNameLabel.setToolTipText("");
        panel2.add(accountNameLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        accountNameField = new JTextField();
        accountNameField.setToolTipText("");
        panel2.add(accountNameField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 5, 5, 5), -1, -1));
        accountEditPanel.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        accountSaveButton = new JButton();
        accountSaveButton.setIcon(new ImageIcon(getClass().getResource("/icon/menu-saveall_dark.png")));
        accountSaveButton.setText("保存");
        panel3.add(accountSaveButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        accountEditScrollPane = new JScrollPane();
        accountEditPanel.add(accountEditScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        accountEditorPanel = new JPanel();
        accountEditorPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1, true, false));
        accountEditScrollPane.setViewportView(accountEditorPanel);
        accountNameLabel.setLabelFor(accountNameField);
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
