package com.fangxuele.tool.push.ui.form;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TMsgKefuPriorityMapper;
import com.fangxuele.tool.push.dao.TTemplateDataMapper;
import com.fangxuele.tool.push.domain.TMsgKefuPriority;
import com.fangxuele.tool.push.domain.TTemplateData;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.component.TableInCellButtonColumn;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;

/**
 * <pre>
 * MessageEditForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/5/6.
 */
@Getter
public class MessageEditForm {
    private JPanel messageEditPanel;
    private JLabel msgNameLabel;
    private JTextField msgNameField;
    private JLabel previewMemberLabel;
    private JTextField previewUserField;
    private JButton previewMsgButton;
    private JButton msgSaveButton;
    private JPanel msgEditorPanel;

    public static MessageEditForm messageEditForm = new MessageEditForm();

    private static TMsgKefuPriorityMapper msgKefuPriorityMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuPriorityMapper.class);
    private static TTemplateDataMapper templateDataMapper = MybatisUtil.getSqlSession().getMapper(TTemplateDataMapper.class);

    /**
     * 初始化消息tab
     */
    public static void init(String selectedMsgName) {
        String msgName;
        if (StringUtils.isEmpty(selectedMsgName)) {
            msgName = App.config.getMsgName();
        } else {
            msgName = selectedMsgName;
        }

        messageEditForm.getMsgNameField().setText(msgName);
        messageEditForm.getPreviewUserField().setText(App.config.getPreviewUser());

        int msgType = App.config.getMsgType();

        if (msgType == MessageTypeEnum.KEFU_CODE) {
            KefuMsgForm.init(msgName);

        } else if (msgType == MessageTypeEnum.KEFU_PRIORITY_CODE) {
            KefuMsgForm.clearAllField();
            MpTemplateMsgForm.clearAllField();
            List<TMsgKefuPriority> tMsgKefuPriorityList = msgKefuPriorityMapper.selectByMsgTypeAndMsgName(msgType, msgName);
            if (tMsgKefuPriorityList.size() > 0) {
                TMsgKefuPriority tMsgKefuPriority = tMsgKefuPriorityList.get(0);
                Integer msgId = tMsgKefuPriority.getId();
                MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateIdTextField().setText(tMsgKefuPriority.getTemplateId());
                MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateUrlTextField().setText(tMsgKefuPriority.getUrl());
                MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateMiniAppidTextField().setText(tMsgKefuPriority.getMaAppid());
                MpTemplateMsgForm.mpTemplateMsgForm.getMsgTemplateMiniPagePathTextField().setText(tMsgKefuPriority.getMaPagePath());

                String kefuMsgType = tMsgKefuPriority.getKefuMsgType();
                KefuMsgForm.kefuMsgForm.getMsgKefuMsgTypeComboBox().setSelectedItem(kefuMsgType);
                if ("文本消息".equals(kefuMsgType)) {
                    KefuMsgForm.kefuMsgForm.getMsgKefuMsgTitleTextField().setText(tMsgKefuPriority.getContent());
                } else if ("图文消息".equals(kefuMsgType)) {
                    KefuMsgForm.kefuMsgForm.getMsgKefuMsgTitleTextField().setText(tMsgKefuPriority.getTitle());
                }
                KefuMsgForm.kefuMsgForm.getMsgKefuPicUrlTextField().setText(tMsgKefuPriority.getImgUrl());
                KefuMsgForm.kefuMsgForm.getMsgKefuDescTextField().setText(tMsgKefuPriority.getDescribe());
                KefuMsgForm.kefuMsgForm.getMsgKefuUrlTextField().setText(tMsgKefuPriority.getKefuUrl());

                KefuMsgForm.switchKefuMsgType(kefuMsgType);

                MpTemplateMsgForm.initTemplateDataTable();
                // 模板消息Data表
                List<TTemplateData> templateDataList = templateDataMapper.selectByMsgTypeAndMsgId(msgType, msgId);
                String[] headerNames = {"Name", "Value", "Color", "操作"};
                Object[][] cellData = new String[templateDataList.size()][headerNames.length];
                for (int i = 0; i < templateDataList.size(); i++) {
                    TTemplateData tTemplateData = templateDataList.get(i);
                    cellData[i][0] = tTemplateData.getName();
                    cellData[i][1] = tTemplateData.getValue();
                    cellData[i][2] = tTemplateData.getColor();
                }
                DefaultTableModel model = new DefaultTableModel(cellData, headerNames);
                MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgDataTable().setModel(model);
                TableColumnModel tableColumnModel = MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgDataTable().getColumnModel();
                tableColumnModel.getColumn(headerNames.length - 1).
                        setCellRenderer(new TableInCellButtonColumn(MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgDataTable(), headerNames.length - 1));
                tableColumnModel.getColumn(headerNames.length - 1).
                        setCellEditor(new TableInCellButtonColumn(MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgDataTable(), headerNames.length - 1));

                // 设置列宽
                tableColumnModel.getColumn(3).setPreferredWidth(130);
                tableColumnModel.getColumn(3).setMaxWidth(130);

                MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgDataTable().updateUI();
            }
        } else if (msgType == MessageTypeEnum.MA_TEMPLATE_CODE) {
            MaTemplateMsgForm.init(msgName);
        } else if (msgType == MessageTypeEnum.MP_TEMPLATE_CODE) {
            MpTemplateMsgForm.init(msgName);
        } else if (msgType == MessageTypeEnum.ALI_TEMPLATE_CODE) {
            AliTemplateMsgForm.init(msgName);
        } else if (msgType == MessageTypeEnum.ALI_YUN_CODE) {
            AliYunMsgForm.init(msgName);
        } else if (msgType == MessageTypeEnum.TX_YUN_CODE) {
            TxYunMsgForm.init(msgName);
        } else if (msgType == MessageTypeEnum.TX_YUN_CODE) {
            YunpianMsgForm.init(msgName);
        }
    }

    /**
     * 根据消息类型转换界面显示
     *
     * @param msgType
     */
    public static void switchMsgType(int msgType) {
        messageEditForm.getMsgEditorPanel().removeAll();
        GridConstraints gridConstraintsRow0 = new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false);
        GridConstraints gridConstraintsRow1 = new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false);
        switch (msgType) {
            case MessageTypeEnum.MP_TEMPLATE_CODE:
                MpTemplateMsgForm.init(null);
                messageEditForm.getMsgEditorPanel().add(MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgPanel(), gridConstraintsRow0);
                messageEditForm.getPreviewMemberLabel().setText("预览用户openid");
                break;
            case MessageTypeEnum.MA_TEMPLATE_CODE:
                MaTemplateMsgForm.init(null);
                messageEditForm.getMsgEditorPanel().add(MaTemplateMsgForm.maTemplateMsgForm.getTemplateMsgPanel(), gridConstraintsRow0);
                messageEditForm.getPreviewMemberLabel().setText("预览用户openid");
                break;
            case MessageTypeEnum.KEFU_CODE:
                KefuMsgForm.init(null);
                messageEditForm.getMsgEditorPanel().add(KefuMsgForm.kefuMsgForm.getKefuMsgPanel(), gridConstraintsRow0);
                messageEditForm.getPreviewMemberLabel().setText("预览用户openid");
                break;
            case MessageTypeEnum.KEFU_PRIORITY_CODE:
                KefuMsgForm.init(null);
                MpTemplateMsgForm.clearAllField();
                messageEditForm.getMsgEditorPanel().add(KefuMsgForm.kefuMsgForm.getKefuMsgPanel(), gridConstraintsRow0);
                messageEditForm.getMsgEditorPanel().add(MpTemplateMsgForm.mpTemplateMsgForm.getTemplateMsgPanel(), gridConstraintsRow1);
                messageEditForm.getPreviewMemberLabel().setText("预览用户openid");
                break;
            case MessageTypeEnum.ALI_YUN_CODE:
                AliYunMsgForm.init(null);
                messageEditForm.getMsgEditorPanel().add(AliYunMsgForm.aliYunMsgForm.getTemplateMsgPanel(), gridConstraintsRow0);
                messageEditForm.getPreviewMemberLabel().setText("预览用户手机号");
                break;
            case MessageTypeEnum.TX_YUN_CODE:
                TxYunMsgForm.init(null);
                messageEditForm.getMsgEditorPanel().add(TxYunMsgForm.txYunMsgForm.getTemplateMsgPanel(), gridConstraintsRow0);
                messageEditForm.getPreviewMemberLabel().setText("预览用户手机号");
                break;
            case MessageTypeEnum.ALI_TEMPLATE_CODE:
                AliTemplateMsgForm.init(null);
                messageEditForm.getMsgEditorPanel().add(AliTemplateMsgForm.aliTemplateMsgForm.getTemplateMsgPanel(), gridConstraintsRow0);
                messageEditForm.getPreviewMemberLabel().setText("预览用户手机号");
                break;
            case MessageTypeEnum.YUN_PIAN_CODE:
                YunpianMsgForm.init(null);
                messageEditForm.getMsgEditorPanel().add(YunpianMsgForm.yunpianMsgForm.getYunpianMsgPanel(), gridConstraintsRow0);
                messageEditForm.getPreviewMemberLabel().setText("预览用户手机号");
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
        messageEditPanel = new JPanel();
        messageEditPanel.setLayout(new GridLayoutManager(3, 5, new Insets(6, 2, 0, 0), -1, -1));
        messageEditPanel.setMaximumSize(new Dimension(-1, -1));
        messageEditPanel.setMinimumSize(new Dimension(-1, -1));
        messageEditPanel.setPreferredSize(new Dimension(-1, -1));
        panel1.add(messageEditPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 4, new Insets(0, 5, 10, 5), -1, -1));
        messageEditPanel.add(panel2, new GridConstraints(0, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        msgNameLabel = new JLabel();
        Font msgNameLabelFont = this.$$$getFont$$$(null, Font.BOLD, -1, msgNameLabel.getFont());
        if (msgNameLabelFont != null) msgNameLabel.setFont(msgNameLabelFont);
        msgNameLabel.setText("消息名称 *");
        msgNameLabel.setToolTipText("给本次推送任务起个名字");
        panel2.add(msgNameLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgNameField = new JTextField();
        msgNameField.setToolTipText("给本次推送任务起个名字");
        panel2.add(msgNameField, new GridConstraints(0, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 5, new Insets(0, 5, 0, 5), -1, -1));
        messageEditPanel.add(panel3, new GridConstraints(2, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        previewMemberLabel = new JLabel();
        previewMemberLabel.setText("预览用户openid/手机号");
        previewMemberLabel.setToolTipText("多个以半角分号分隔");
        panel3.add(previewMemberLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        previewUserField = new JTextField();
        previewUserField.setToolTipText("多个以半角分号分隔");
        panel3.add(previewUserField, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        previewMsgButton = new JButton();
        previewMsgButton.setIcon(new ImageIcon(getClass().getResource("/icon/preview_dark.png")));
        previewMsgButton.setText("预览");
        panel3.add(previewMsgButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgSaveButton = new JButton();
        msgSaveButton.setIcon(new ImageIcon(getClass().getResource("/icon/menu-saveall_dark.png")));
        msgSaveButton.setText("保存");
        panel3.add(msgSaveButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgEditorPanel = new JPanel();
        msgEditorPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        messageEditPanel.add(msgEditorPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        msgNameLabel.setLabelFor(msgNameField);
        previewMemberLabel.setLabelFor(previewUserField);
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
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

}
