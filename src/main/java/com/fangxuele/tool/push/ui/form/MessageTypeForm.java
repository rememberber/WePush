package com.fangxuele.tool.push.ui.form;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * <pre>
 * MessageTypeForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/5/7.
 */
@Getter
public class MessageTypeForm {
    private JPanel messageTypePanel;
    private JRadioButton mpTemplateRadioButton;
    private JRadioButton maTemplateRadioButton;
    private JRadioButton kefuRadioButton;
    private JRadioButton kefuPriorityRadioButton;
    private JRadioButton aliYunRadioButton;
    private JRadioButton txYunRadioButton;
    private JRadioButton yunPianRadioButton;
    private JRadioButton upYunRadioButton;
    private JRadioButton hwYunRadioButton;
    private JRadioButton eMailRadioButton;
    private JPanel msgTypeListPanel;
    private JRadioButton 网易云信短信RadioButton;
    private JRadioButton 榛子云短信RadioButton;
    private JRadioButton luosimao短信RadioButton;
    private JRadioButton 极光短信RadioButton;
    private JRadioButton 极光推送RadioButton;
    private JRadioButton wxCpRadioButton;
    private JRadioButton dingRadioButton;
    private JScrollPane messageTypeScrollPane;
    private JLabel kefuPriorityTipsLabel;
    private JRadioButton httpRadioButton;
    private JRadioButton qiniuRadioButton;
    private JRadioButton bdYunRadioButton;
    private JRadioButton wxUniformMessageRadioButton;
    private JRadioButton maSubscribeRadioButton;
    private JRadioButton mpSubscribeRadioButton;

    private static MessageTypeForm messageTypeForm;

    private MessageTypeForm() {
    }

    public static MessageTypeForm getInstance() {
        if (messageTypeForm == null) {
            messageTypeForm = new MessageTypeForm();
        }
        return messageTypeForm;
    }

    /**
     * 初始化消息类型tab
     */
    public static void init() {
        messageTypeForm = getInstance();

        messageTypeForm.getMessageTypeScrollPane().getVerticalScrollBar().setUnitIncrement(15);
        messageTypeForm.getMessageTypeScrollPane().getVerticalScrollBar().setDoubleBuffered(true);

        int msgType = App.config.getMsgType();
        clearAllSelected();

        switch (msgType) {
            case MessageTypeEnum.MP_TEMPLATE_CODE:
                messageTypeForm.getMpTemplateRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.MP_SUBSCRIBE_CODE:
                messageTypeForm.getMpSubscribeRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.MA_TEMPLATE_CODE:
                messageTypeForm.getMaTemplateRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.MA_SUBSCRIBE_CODE:
                messageTypeForm.getMaSubscribeRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.KEFU_CODE:
                messageTypeForm.getKefuRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.KEFU_PRIORITY_CODE:
                messageTypeForm.getKefuPriorityRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.WX_UNIFORM_MESSAGE_CODE:
                messageTypeForm.getWxUniformMessageRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.ALI_YUN_CODE:
                messageTypeForm.getAliYunRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.TX_YUN_CODE:
                messageTypeForm.getTxYunRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.QI_NIU_YUN_CODE:
                messageTypeForm.getQiniuRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.YUN_PIAN_CODE:
                messageTypeForm.getYunPianRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.UP_YUN_CODE:
                messageTypeForm.getUpYunRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.HW_YUN_CODE:
                messageTypeForm.getHwYunRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.EMAIL_CODE:
                messageTypeForm.getEMailRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.WX_CP_CODE:
                messageTypeForm.getWxCpRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.HTTP_CODE:
                messageTypeForm.getHttpRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.DING_CODE:
                messageTypeForm.getDingRadioButton().setSelected(true);
                break;
            case MessageTypeEnum.BD_YUN_CODE:
                messageTypeForm.getBdYunRadioButton().setSelected(true);
                break;

            default:
        }
        initMessageManageFormLayOut(msgType);
        initMessageEditFormLayOut(msgType);
        initMemberFormLayOut(msgType);
        MessageEditForm.switchMsgType(msgType);
        MessageEditForm.getInstance().getMsgTypeName().setText(MessageTypeEnum.getName(msgType));
        MessageManageForm.init();
        MessageEditForm.getInstance().getMsgNameField().setText("");
        MemberForm.init();
        PushHisForm.init();
        ScheduleForm.init();
    }

    private static void initMessageManageFormLayOut(int msgType) {
        if (MessageTypeEnum.isWxMaOrMpType(msgType)) {
            MessageManageForm.getInstance().getAccountSwitchPanel().setVisible(true);
        } else {
            MessageManageForm.getInstance().getAccountSwitchPanel().setVisible(false);
        }
    }

    private static void initMessageEditFormLayOut(int msgType) {
        if (msgType == MessageTypeEnum.HTTP_CODE) {
            MessageEditForm.getInstance().getPreviewMemberLabel().setText("消息变量");
            MessageEditForm.getInstance().getPreviewMsgButton().setText("发送请求");
        } else {
            MessageEditForm.getInstance().getPreviewMemberLabel().setText("预览用户");
            MessageEditForm.getInstance().getPreviewMsgButton().setText("预览");
        }
    }

    private static void initMemberFormLayOut(int msgType) {
        Component[] components = MemberForm.getInstance().getImportWayPanel().getComponents();
        for (Component component : components) {
            if (component instanceof JPanel) {
                component.setVisible(false);
            }
        }
        MemberForm.getInstance().getMemberTabDownPanel().setVisible(true);
        MemberForm.getInstance().getMemberTabCenterPanel().setVisible(true);
        if (MessageTypeEnum.isWxMaOrMpType(msgType)) {
            MemberForm.getInstance().getImportFromWeixinPanel().setVisible(true);
            MemberForm.getInstance().getImportOptionPanel().setVisible(true);
        }
        if (msgType == MessageTypeEnum.WX_CP_CODE) {
            MemberForm.getInstance().getImportFromWxCpPanel().setVisible(true);
        }
        if (msgType == MessageTypeEnum.DING_CODE) {
            MemberForm.getInstance().getImportFromDingPanel().setVisible(true);
        }

        if (msgType == MessageTypeEnum.HTTP_CODE) {
            MainWindow.getInstance().getTabbedPane().setTitleAt(3, "③准备消息变量");
            MemberForm.getInstance().getImportFromNumPanel().setVisible(true);
            PushForm.getInstance().getSaveResponseBodyCheckBox().setVisible(true);
            InfinityForm.getInstance().getSaveResponseBodyCheckBox().setVisible(true);
        } else {
            MainWindow.getInstance().getTabbedPane().setTitleAt(3, "③准备目标用户");
            PushForm.getInstance().getSaveResponseBodyCheckBox().setVisible(false);
            InfinityForm.getInstance().getSaveResponseBodyCheckBox().setVisible(false);
        }
    }

    /**
     * 清除所有radio选中状态
     */
    public static void clearAllSelected() {
        messageTypeForm = getInstance();

        Component[] components = messageTypeForm.getMsgTypeListPanel().getComponents();
        for (Component component : components) {
            if (component instanceof JRadioButton) {
                ((JRadioButton) component).setSelected(false);
            }
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
        messageTypePanel = new JPanel();
        messageTypePanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        messageTypePanel.setAutoscrolls(false);
        messageTypeScrollPane = new JScrollPane();
        messageTypeScrollPane.setAutoscrolls(true);
        messageTypePanel.add(messageTypeScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        msgTypeListPanel = new JPanel();
        msgTypeListPanel.setLayout(new GridLayoutManager(23, 3, new Insets(20, 20, 0, 0), -1, -1));
        msgTypeListPanel.setAutoscrolls(true);
        messageTypeScrollPane.setViewportView(msgTypeListPanel);
        mpTemplateRadioButton = new JRadioButton();
        mpTemplateRadioButton.setEnabled(true);
        mpTemplateRadioButton.setText("公众号-模板消息");
        msgTypeListPanel.add(mpTemplateRadioButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mpSubscribeRadioButton = new JRadioButton();
        mpSubscribeRadioButton.setEnabled(true);
        mpSubscribeRadioButton.setText("公众号-订阅通知");
        msgTypeListPanel.add(mpSubscribeRadioButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maTemplateRadioButton = new JRadioButton();
        maTemplateRadioButton.setEnabled(false);
        maTemplateRadioButton.setText("小程序-模板消息");
        msgTypeListPanel.add(maTemplateRadioButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maSubscribeRadioButton = new JRadioButton();
        maSubscribeRadioButton.setText("小程序-订阅消息");
        msgTypeListPanel.add(maSubscribeRadioButton, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        kefuRadioButton = new JRadioButton();
        kefuRadioButton.setText("公众号-客服消息");
        msgTypeListPanel.add(kefuRadioButton, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        kefuPriorityRadioButton = new JRadioButton();
        kefuPriorityRadioButton.setText("公众号-客服消息优先");
        kefuPriorityRadioButton.setToolTipText("优先尝试发送客服消息，如果失败则发送模板消息");
        msgTypeListPanel.add(kefuPriorityRadioButton, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        aliYunRadioButton = new JRadioButton();
        aliYunRadioButton.setText("阿里云短信");
        msgTypeListPanel.add(aliYunRadioButton, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txYunRadioButton = new JRadioButton();
        txYunRadioButton.setText("腾讯云短信");
        msgTypeListPanel.add(txYunRadioButton, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        yunPianRadioButton = new JRadioButton();
        yunPianRadioButton.setText("云片网短信");
        msgTypeListPanel.add(yunPianRadioButton, new GridConstraints(14, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        网易云信短信RadioButton = new JRadioButton();
        网易云信短信RadioButton.setEnabled(false);
        网易云信短信RadioButton.setText("网易云信短信");
        msgTypeListPanel.add(网易云信短信RadioButton, new GridConstraints(18, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        榛子云短信RadioButton = new JRadioButton();
        榛子云短信RadioButton.setEnabled(false);
        榛子云短信RadioButton.setText("榛子云短信");
        msgTypeListPanel.add(榛子云短信RadioButton, new GridConstraints(19, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        luosimao短信RadioButton = new JRadioButton();
        luosimao短信RadioButton.setEnabled(false);
        luosimao短信RadioButton.setText("Luosimao短信");
        msgTypeListPanel.add(luosimao短信RadioButton, new GridConstraints(20, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        极光短信RadioButton = new JRadioButton();
        极光短信RadioButton.setEnabled(false);
        极光短信RadioButton.setText("极光短信");
        msgTypeListPanel.add(极光短信RadioButton, new GridConstraints(21, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        极光推送RadioButton = new JRadioButton();
        极光推送RadioButton.setEnabled(false);
        极光推送RadioButton.setText("极光推送");
        msgTypeListPanel.add(极光推送RadioButton, new GridConstraints(22, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        kefuPriorityTipsLabel = new JLabel();
        kefuPriorityTipsLabel.setIcon(new ImageIcon(getClass().getResource("/icon/helpButton.png")));
        kefuPriorityTipsLabel.setText("");
        msgTypeListPanel.add(kefuPriorityTipsLabel, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        eMailRadioButton = new JRadioButton();
        eMailRadioButton.setEnabled(true);
        eMailRadioButton.setText("E-Mail（BETA）");
        msgTypeListPanel.add(eMailRadioButton, new GridConstraints(17, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        wxCpRadioButton = new JRadioButton();
        wxCpRadioButton.setEnabled(true);
        wxCpRadioButton.setText("企业号/企业微信");
        msgTypeListPanel.add(wxCpRadioButton, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        httpRadioButton = new JRadioButton();
        httpRadioButton.setEnabled(true);
        httpRadioButton.setText("HTTP请求");
        msgTypeListPanel.add(httpRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        msgTypeListPanel.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        dingRadioButton = new JRadioButton();
        dingRadioButton.setEnabled(true);
        dingRadioButton.setText("钉钉");
        msgTypeListPanel.add(dingRadioButton, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hwYunRadioButton = new JRadioButton();
        hwYunRadioButton.setEnabled(true);
        hwYunRadioButton.setText("华为云短信（BETA）");
        msgTypeListPanel.add(hwYunRadioButton, new GridConstraints(12, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bdYunRadioButton = new JRadioButton();
        bdYunRadioButton.setEnabled(true);
        bdYunRadioButton.setText("百度云短信（BETA）");
        msgTypeListPanel.add(bdYunRadioButton, new GridConstraints(13, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        upYunRadioButton = new JRadioButton();
        upYunRadioButton.setEnabled(true);
        upYunRadioButton.setText("又拍云短信（BETA）");
        msgTypeListPanel.add(upYunRadioButton, new GridConstraints(15, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        qiniuRadioButton = new JRadioButton();
        qiniuRadioButton.setEnabled(true);
        qiniuRadioButton.setText("七牛云短信（BETA）");
        msgTypeListPanel.add(qiniuRadioButton, new GridConstraints(16, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        wxUniformMessageRadioButton = new JRadioButton();
        wxUniformMessageRadioButton.setText("小程序-统一服务消息（BETA）");
        msgTypeListPanel.add(wxUniformMessageRadioButton, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(8, 8, 8, 0), -1, -1));
        messageTypePanel.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("WePush目前仅是各类消息官方SDK的一种实现，");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("使用前请仔细查看该消息平台官网的使用规则和开发文档，尤其是发送频率限制等，避免造成不必要的麻烦");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return messageTypePanel;
    }

}
