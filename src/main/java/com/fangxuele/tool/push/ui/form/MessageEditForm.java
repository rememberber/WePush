package com.fangxuele.tool.push.ui.form;

import com.fangxuele.tool.push.dao.TMsgKefuMapper;
import com.fangxuele.tool.push.dao.TMsgKefuPriorityMapper;
import com.fangxuele.tool.push.dao.TMsgMaTemplateMapper;
import com.fangxuele.tool.push.dao.TMsgMpTemplateMapper;
import com.fangxuele.tool.push.dao.TMsgSmsMapper;
import com.fangxuele.tool.push.dao.TTemplateDataMapper;
import com.fangxuele.tool.push.domain.TMsgKefu;
import com.fangxuele.tool.push.domain.TMsgKefuPriority;
import com.fangxuele.tool.push.domain.TMsgMaTemplate;
import com.fangxuele.tool.push.domain.TMsgMpTemplate;
import com.fangxuele.tool.push.domain.TMsgSms;
import com.fangxuele.tool.push.domain.TTemplateData;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.component.TableInCellButtonColumn;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
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
    private JLabel templateMiniProgramAppidLabel;
    private JTextField msgTemplateMiniAppidTextField;
    private JLabel templateMiniProgramPagePathLabel;
    private JTextField msgTemplateMiniPagePathTextField;
    private JLabel templateMiniProgramOptionalLabel1;
    private JLabel templateMiniProgramOptionalLabel2;
    private JLabel templateKeyWordLabel;
    private JTextField msgTemplateKeyWordTextField;
    private JPanel kefuMsgPanel;
    private JLabel kefuMsgTypeLabel;
    private JComboBox msgKefuMsgTypeComboBox;
    private JLabel kefuMsgTitleLabel;
    private JTextField msgKefuMsgTitleTextField;
    private JLabel kefuMsgPicUrlLabel;
    private JLabel kefuMsgDescLabel;
    private JTextField msgKefuPicUrlTextField;
    private JTextField msgKefuDescTextField;
    private JLabel kefuMsgUrlLabel;
    private JTextField msgKefuUrlTextField;
    private JPanel yunpianMsgPanel;
    private JTextArea msgYunpianMsgContentTextField;

    public static MessageEditForm messageEditForm = new MessageEditForm();

    private static TMsgKefuMapper msgKefuMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuMapper.class);
    private static TMsgKefuPriorityMapper msgKefuPriorityMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuPriorityMapper.class);
    private static TMsgMaTemplateMapper msgMaTemplateMapper = MybatisUtil.getSqlSession().getMapper(TMsgMaTemplateMapper.class);
    private static TMsgMpTemplateMapper msgMpTemplateMapper = MybatisUtil.getSqlSession().getMapper(TMsgMpTemplateMapper.class);
    private static TMsgSmsMapper msgSmsMapper = MybatisUtil.getSqlSession().getMapper(TMsgSmsMapper.class);
    private static TTemplateDataMapper templateDataMapper = MybatisUtil.getSqlSession().getMapper(TTemplateDataMapper.class);

    /**
     * 初始化消息tab
     */
    public static void init(String selectedMsgName) {
        // 初始化，清空所有相关的输入框内容
        clearAllField();

        String msgName;
        if (StringUtils.isEmpty(selectedMsgName)) {
            msgName = Init.config.getMsgName();
        } else {
            msgName = selectedMsgName;
        }

        messageEditForm.getMsgNameField().setText(msgName);
        messageEditForm.getPreviewUserField().setText(Init.config.getPreviewUser());

        int msgType = Init.config.getMsgType();

        int msgId = 0;
        if (msgType == MessageTypeEnum.KEFU_CODE) {
            List<TMsgKefu> tMsgKefuList = msgKefuMapper.selectByMsgTypeAndMsgName(msgType, msgName);
            if (tMsgKefuList.size() > 0) {
                TMsgKefu tMsgKefu = tMsgKefuList.get(0);
                msgId = tMsgKefu.getId();
                String kefuMsgType = tMsgKefu.getKefuMsgType();
                messageEditForm.getMsgKefuMsgTypeComboBox().setSelectedItem(kefuMsgType);
                if ("文本消息".equals(kefuMsgType)) {
                    messageEditForm.getMsgKefuMsgTitleTextField().setText(tMsgKefu.getContent());
                } else if ("图文消息".equals(kefuMsgType)) {
                    messageEditForm.getMsgKefuMsgTitleTextField().setText(tMsgKefu.getTitle());
                }
                messageEditForm.getMsgKefuPicUrlTextField().setText(tMsgKefu.getImgUrl());
                messageEditForm.getMsgKefuDescTextField().setText(tMsgKefu.getDescribe());
                messageEditForm.getMsgKefuUrlTextField().setText(tMsgKefu.getUrl());

                switchKefuMsgType(kefuMsgType);
            }
        } else if (msgType == MessageTypeEnum.KEFU_PRIORITY_CODE) {
            List<TMsgKefuPriority> tMsgKefuPriorityList = msgKefuPriorityMapper.selectByMsgTypeAndMsgName(msgType, msgName);
            if (tMsgKefuPriorityList.size() > 0) {
                TMsgKefuPriority tMsgKefuPriority = tMsgKefuPriorityList.get(0);
                msgId = tMsgKefuPriority.getId();
                messageEditForm.getMsgTemplateIdTextField().setText(tMsgKefuPriority.getTemplateId());
                messageEditForm.getMsgTemplateUrlTextField().setText(tMsgKefuPriority.getUrl());
                messageEditForm.getMsgTemplateMiniAppidTextField().setText(tMsgKefuPriority.getMaAppid());
                messageEditForm.getMsgTemplateMiniPagePathTextField().setText(tMsgKefuPriority.getMaPagePath());

                String kefuMsgType = tMsgKefuPriority.getKefuMsgType();
                messageEditForm.getMsgKefuMsgTypeComboBox().setSelectedItem(kefuMsgType);
                if ("文本消息".equals(kefuMsgType)) {
                    messageEditForm.getMsgKefuMsgTitleTextField().setText(tMsgKefuPriority.getContent());
                } else if ("图文消息".equals(kefuMsgType)) {
                    messageEditForm.getMsgKefuMsgTitleTextField().setText(tMsgKefuPriority.getTitle());
                }
                messageEditForm.getMsgKefuPicUrlTextField().setText(tMsgKefuPriority.getImgUrl());
                messageEditForm.getMsgKefuDescTextField().setText(tMsgKefuPriority.getDescribe());
                messageEditForm.getMsgKefuUrlTextField().setText(tMsgKefuPriority.getKefuUrl());

                switchKefuMsgType(kefuMsgType);
            }
        } else if (msgType == MessageTypeEnum.MA_TEMPLATE_CODE) {
            List<TMsgMaTemplate> tMsgMaTemplateList = msgMaTemplateMapper.selectByMsgTypeAndMsgName(msgType, msgName);
            if (tMsgMaTemplateList.size() > 0) {
                TMsgMaTemplate tMsgMaTemplate = tMsgMaTemplateList.get(0);
                msgId = tMsgMaTemplate.getId();
                messageEditForm.getMsgTemplateIdTextField().setText(tMsgMaTemplate.getTemplateId());
                messageEditForm.getMsgTemplateUrlTextField().setText(tMsgMaTemplate.getPage());
                messageEditForm.getMsgTemplateKeyWordTextField().setText(tMsgMaTemplate.getEmphasisKeyword());
            }
        } else if (msgType == MessageTypeEnum.MP_TEMPLATE_CODE) {
            List<TMsgMpTemplate> tMsgMpTemplateList = msgMpTemplateMapper.selectByMsgTypeAndMsgName(msgType, msgName);
            if (tMsgMpTemplateList.size() > 0) {
                TMsgMpTemplate tMsgMpTemplate = tMsgMpTemplateList.get(0);
                msgId = tMsgMpTemplate.getId();
                messageEditForm.getMsgTemplateIdTextField().setText(tMsgMpTemplate.getTemplateId());
                messageEditForm.getMsgTemplateUrlTextField().setText(tMsgMpTemplate.getUrl());
                messageEditForm.getMsgTemplateMiniAppidTextField().setText(tMsgMpTemplate.getMaAppid());
                messageEditForm.getMsgTemplateMiniPagePathTextField().setText(tMsgMpTemplate.getMaPagePath());
            }
        } else {
            List<TMsgSms> tMsgSmsList = msgSmsMapper.selectByMsgTypeAndMsgName(msgType, msgName);
            if (tMsgSmsList.size() > 0) {
                TMsgSms tMsgSms = tMsgSmsList.get(0);
                msgId = tMsgSms.getId();
                if (msgType == MessageTypeEnum.YUN_PIAN_CODE) {
                    messageEditForm.getMsgYunpianMsgContentTextField().setText(tMsgSms.getContent());
                } else {
                    messageEditForm.getMsgTemplateIdTextField().setText(tMsgSms.getTemplateId());
                }
            }
        }
        if (msgType != MessageTypeEnum.KEFU_CODE) {
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
            messageEditForm.getTemplateMsgDataTable().setModel(model);
            messageEditForm.getTemplateMsgDataTable().getColumnModel().
                    getColumn(headerNames.length - 1).
                    setCellRenderer(new TableInCellButtonColumn(messageEditForm.getTemplateMsgDataTable(), headerNames.length - 1));
            messageEditForm.getTemplateMsgDataTable().getColumnModel().
                    getColumn(headerNames.length - 1).
                    setCellEditor(new TableInCellButtonColumn(messageEditForm.getTemplateMsgDataTable(), headerNames.length - 1));

            // 设置列宽
            messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(0).setPreferredWidth(150);
            messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(0).setMaxWidth(150);
            messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(2).setPreferredWidth(130);
            messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(2).setMaxWidth(130);
            messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(3).setPreferredWidth(130);
            messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(3).setMaxWidth(130);

            messageEditForm.getTemplateMsgDataTable().updateUI();
        }
    }

    /**
     * 清空所有界面字段
     */
    public static void clearAllField() {
        messageEditForm.getMsgNameField().setText("");
        messageEditForm.getMsgTemplateIdTextField().setText("");
        messageEditForm.getMsgTemplateUrlTextField().setText("");
        messageEditForm.getMsgKefuMsgTitleTextField().setText("");
        messageEditForm.getMsgKefuPicUrlTextField().setText("");
        messageEditForm.getMsgKefuDescTextField().setText("");
        messageEditForm.getMsgKefuUrlTextField().setText("");
        messageEditForm.getMsgTemplateMiniAppidTextField().setText("");
        messageEditForm.getMsgTemplateMiniPagePathTextField().setText("");
        messageEditForm.getMsgTemplateKeyWordTextField().setText("");
        messageEditForm.getMsgYunpianMsgContentTextField().setText("");
        messageEditForm.getTemplateDataNameTextField().setText("");
        messageEditForm.getTemplateDataValueTextField().setText("");
        messageEditForm.getTemplateDataColorTextField().setText("");
        messageEditForm.getPreviewUserField().setText("");
        initTemplateDataTable();
    }

    /**
     * 根据消息类型转换界面显示
     *
     * @param msgType
     */
    public static void switchMsgType(int msgType) {
        messageEditForm.getKefuMsgPanel().setVisible(false);
        messageEditForm.getTemplateMsgPanel().setVisible(false);
        messageEditForm.getYunpianMsgPanel().setVisible(false);
        switch (msgType) {
            case MessageTypeEnum.MP_TEMPLATE_CODE:
                messageEditForm.getTemplateMsgPanel().setVisible(true);
                messageEditForm.getTemplateUrlLabel().setVisible(true);
                messageEditForm.getMsgTemplateUrlTextField().setVisible(true);
                messageEditForm.getTemplateMiniProgramAppidLabel().setVisible(true);
                messageEditForm.getMsgTemplateMiniAppidTextField().setVisible(true);
                messageEditForm.getTemplateMiniProgramPagePathLabel().setVisible(true);
                messageEditForm.getMsgTemplateMiniPagePathTextField().setVisible(true);
                messageEditForm.getTemplateMiniProgramOptionalLabel1().setVisible(true);
                messageEditForm.getTemplateMiniProgramOptionalLabel2().setVisible(true);
                messageEditForm.getTemplateMsgColorLabel().setVisible(true);
                messageEditForm.getTemplateDataColorTextField().setVisible(true);
                messageEditForm.getMsgTemplateKeyWordTextField().setVisible(false);
                messageEditForm.getTemplateKeyWordLabel().setVisible(false);
                messageEditForm.getPreviewMemberLabel().setText("预览消息用户openid（多个以半角分号分隔）");
                break;
            case MessageTypeEnum.MA_TEMPLATE_CODE:
                messageEditForm.getTemplateMsgPanel().setVisible(true);
                messageEditForm.getTemplateUrlLabel().setVisible(true);
                messageEditForm.getMsgTemplateUrlTextField().setVisible(true);
                messageEditForm.getTemplateMiniProgramAppidLabel().setVisible(false);
                messageEditForm.getMsgTemplateMiniAppidTextField().setVisible(false);
                messageEditForm.getTemplateMiniProgramPagePathLabel().setVisible(false);
                messageEditForm.getMsgTemplateMiniPagePathTextField().setVisible(false);
                messageEditForm.getTemplateMiniProgramOptionalLabel1().setVisible(false);
                messageEditForm.getTemplateMiniProgramOptionalLabel2().setVisible(false);
                messageEditForm.getTemplateMsgColorLabel().setVisible(true);
                messageEditForm.getTemplateDataColorTextField().setVisible(true);
                messageEditForm.getMsgTemplateKeyWordTextField().setVisible(true);
                messageEditForm.getTemplateKeyWordLabel().setVisible(true);
                messageEditForm.getPreviewMemberLabel().setText("预览消息用户openid（多个以半角分号分隔）");
                break;
            case MessageTypeEnum.KEFU_CODE:
                messageEditForm.getKefuMsgPanel().setVisible(true);
                messageEditForm.getPreviewMemberLabel().setText("预览消息用户openid（多个以半角分号分隔）");
                break;
            case MessageTypeEnum.KEFU_PRIORITY_CODE:
                messageEditForm.getKefuMsgPanel().setVisible(true);
                messageEditForm.getTemplateMsgPanel().setVisible(true);
                messageEditForm.getTemplateUrlLabel().setVisible(true);
                messageEditForm.getMsgTemplateUrlTextField().setVisible(true);
                messageEditForm.getTemplateMiniProgramAppidLabel().setVisible(true);
                messageEditForm.getMsgTemplateMiniAppidTextField().setVisible(true);
                messageEditForm.getTemplateMiniProgramPagePathLabel().setVisible(true);
                messageEditForm.getMsgTemplateMiniPagePathTextField().setVisible(true);
                messageEditForm.getTemplateMiniProgramOptionalLabel1().setVisible(true);
                messageEditForm.getTemplateMiniProgramOptionalLabel2().setVisible(true);
                messageEditForm.getTemplateMsgColorLabel().setVisible(true);
                messageEditForm.getTemplateDataColorTextField().setVisible(true);
                messageEditForm.getMsgTemplateKeyWordTextField().setVisible(false);
                messageEditForm.getTemplateKeyWordLabel().setVisible(false);
                messageEditForm.getPreviewMemberLabel().setText("预览消息用户openid（多个以半角分号分隔）");
                break;
            case MessageTypeEnum.ALI_YUN_CODE:
            case MessageTypeEnum.TX_YUN_CODE:
            case MessageTypeEnum.ALI_TEMPLATE_CODE:
                messageEditForm.getTemplateMsgPanel().setVisible(true);
                messageEditForm.getTemplateUrlLabel().setVisible(false);
                messageEditForm.getMsgTemplateUrlTextField().setVisible(false);
                messageEditForm.getTemplateMiniProgramAppidLabel().setVisible(false);
                messageEditForm.getMsgTemplateMiniAppidTextField().setVisible(false);
                messageEditForm.getTemplateMiniProgramPagePathLabel().setVisible(false);
                messageEditForm.getMsgTemplateMiniPagePathTextField().setVisible(false);
                messageEditForm.getTemplateMiniProgramOptionalLabel1().setVisible(false);
                messageEditForm.getTemplateMiniProgramOptionalLabel2().setVisible(false);
                messageEditForm.getTemplateMsgColorLabel().setVisible(false);
                messageEditForm.getTemplateDataColorTextField().setVisible(false);
                messageEditForm.getMsgTemplateKeyWordTextField().setVisible(false);
                messageEditForm.getTemplateKeyWordLabel().setVisible(false);
                messageEditForm.getPreviewMemberLabel().setText("预览消息用户手机号（多个以半角分号分隔）");
                break;
            case MessageTypeEnum.YUN_PIAN_CODE:
                messageEditForm.getYunpianMsgPanel().setVisible(true);
                messageEditForm.getPreviewMemberLabel().setText("预览消息用户手机号（多个以半角分号分隔）");
                break;
            default:
                break;
        }
    }

    /**
     * 根据客服消息类型转换界面显示
     *
     * @param msgType 消息类型
     */
    public static void switchKefuMsgType(String msgType) {
        switch (msgType) {
            case "文本消息":
                messageEditForm.getKefuMsgTitleLabel().setText("内容");
                messageEditForm.getKefuMsgDescLabel().setVisible(false);
                messageEditForm.getMsgKefuDescTextField().setVisible(false);
                messageEditForm.getKefuMsgPicUrlLabel().setVisible(false);
                messageEditForm.getMsgKefuPicUrlTextField().setVisible(false);
                messageEditForm.getMsgKefuDescTextField().setVisible(false);
                messageEditForm.getKefuMsgUrlLabel().setVisible(false);
                messageEditForm.getMsgKefuUrlTextField().setVisible(false);
                break;
            case "图文消息":
                messageEditForm.getKefuMsgTitleLabel().setText("标题");
                messageEditForm.getKefuMsgDescLabel().setVisible(true);
                messageEditForm.getMsgKefuDescTextField().setVisible(true);
                messageEditForm.getKefuMsgPicUrlLabel().setVisible(true);
                messageEditForm.getMsgKefuPicUrlTextField().setVisible(true);
                messageEditForm.getMsgKefuDescTextField().setVisible(true);
                messageEditForm.getKefuMsgUrlLabel().setVisible(true);
                messageEditForm.getMsgKefuUrlTextField().setVisible(true);
                break;
            default:
                break;
        }
    }

    /**
     * 初始化模板消息数据table
     */
    public static void initTemplateDataTable() {
        String[] headerNames = {"Name", "Value", "Color", "操作"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        messageEditForm.getTemplateMsgDataTable().setModel(model);
        messageEditForm.getTemplateMsgDataTable().updateUI();
        messageEditForm.getTemplateMsgDataTable().getColumnModel().
                getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(messageEditForm.getTemplateMsgDataTable(), headerNames.length - 1));
        messageEditForm.getTemplateMsgDataTable().getColumnModel().
                getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(messageEditForm.getTemplateMsgDataTable(), headerNames.length - 1));

        // 设置列宽
        messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(0).setPreferredWidth(150);
        messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(0).setMaxWidth(150);
        messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(2).setPreferredWidth(130);
        messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(2).setMaxWidth(130);
        messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(3).setPreferredWidth(130);
        messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(3).setMaxWidth(130);
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
        messageEditPanel.setLayout(new GridLayoutManager(4, 5, new Insets(10, 8, 0, 8), -1, -1));
        messageEditPanel.setMaximumSize(new Dimension(-1, -1));
        messageEditPanel.setMinimumSize(new Dimension(-1, -1));
        messageEditPanel.setPreferredSize(new Dimension(-1, -1));
        panel1.add(messageEditPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 10, 0), -1, -1));
        messageEditPanel.add(panel2, new GridConstraints(0, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        msgNameLabel = new JLabel();
        Font msgNameLabelFont = this.$$$getFont$$$(null, Font.BOLD, -1, msgNameLabel.getFont());
        if (msgNameLabelFont != null) msgNameLabel.setFont(msgNameLabelFont);
        msgNameLabel.setText("消息名称");
        panel2.add(msgNameLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgNameField = new JTextField();
        panel2.add(msgNameField, new GridConstraints(0, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 5, new Insets(0, 10, 0, 0), -1, -1));
        messageEditPanel.add(panel3, new GridConstraints(3, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        previewMemberLabel = new JLabel();
        previewMemberLabel.setText("预览用户openid/手机号（以半角分号分隔）");
        panel3.add(previewMemberLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        previewUserField = new JTextField();
        panel3.add(previewUserField, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        previewMsgButton = new JButton();
        previewMsgButton.setText("预览");
        panel3.add(previewMsgButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgSaveButton = new JButton();
        msgSaveButton.setText("保存");
        panel3.add(msgSaveButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        templateMsgPanel = new JPanel();
        templateMsgPanel.setLayout(new GridLayoutManager(6, 3, new Insets(10, 10, 0, 0), -1, -1));
        messageEditPanel.add(templateMsgPanel, new GridConstraints(1, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        templateMsgPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "模板消息编辑", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, templateMsgPanel.getFont())));
        templateIdLabel = new JLabel();
        templateIdLabel.setText("模板ID");
        templateMsgPanel.add(templateIdLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgTemplateIdTextField = new JTextField();
        templateMsgPanel.add(msgTemplateIdTextField, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        templateUrlLabel = new JLabel();
        templateUrlLabel.setText("跳转URL");
        templateMsgPanel.add(templateUrlLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgTemplateUrlTextField = new JTextField();
        templateMsgPanel.add(msgTemplateUrlTextField, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        templateMsgDataPanel = new JPanel();
        templateMsgDataPanel.setLayout(new GridLayoutManager(2, 7, new Insets(10, 0, 0, 0), -1, -1));
        templateMsgPanel.add(templateMsgDataPanel, new GridConstraints(5, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        templateMsgDataPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "模板变量（可使用\"$ENTER$\"作为换行符）", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.ABOVE_TOP));
        templateMsgNameLabel = new JLabel();
        templateMsgNameLabel.setText("name");
        templateMsgDataPanel.add(templateMsgNameLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        templateDataNameTextField = new JTextField();
        templateMsgDataPanel.add(templateDataNameTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        templateMsgValueLabel = new JLabel();
        templateMsgValueLabel.setText("value");
        templateMsgDataPanel.add(templateMsgValueLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        templateDataValueTextField = new JTextField();
        templateMsgDataPanel.add(templateDataValueTextField, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        templateMsgColorLabel = new JLabel();
        templateMsgColorLabel.setText("color");
        templateMsgDataPanel.add(templateMsgColorLabel, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        templateDataColorTextField = new JTextField();
        templateMsgDataPanel.add(templateDataColorTextField, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        templateMsgDataAddButton = new JButton();
        templateMsgDataAddButton.setIcon(new ImageIcon(getClass().getResource("/icon/add.png")));
        templateMsgDataAddButton.setText("添加");
        templateMsgDataPanel.add(templateMsgDataAddButton, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        templateMsgDataPanel.add(scrollPane1, new GridConstraints(1, 0, 1, 7, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        templateMsgDataTable = new JTable();
        templateMsgDataTable.setAutoCreateColumnsFromModel(true);
        templateMsgDataTable.setAutoCreateRowSorter(true);
        templateMsgDataTable.setGridColor(new Color(-12236470));
        templateMsgDataTable.setRowHeight(40);
        scrollPane1.setViewportView(templateMsgDataTable);
        templateMiniProgramAppidLabel = new JLabel();
        templateMiniProgramAppidLabel.setText("小程序appid");
        templateMsgPanel.add(templateMiniProgramAppidLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgTemplateMiniAppidTextField = new JTextField();
        msgTemplateMiniAppidTextField.setText("");
        templateMsgPanel.add(msgTemplateMiniAppidTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        templateMiniProgramPagePathLabel = new JLabel();
        templateMiniProgramPagePathLabel.setText("小程序页面路径");
        templateMsgPanel.add(templateMiniProgramPagePathLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgTemplateMiniPagePathTextField = new JTextField();
        msgTemplateMiniPagePathTextField.setText("");
        templateMsgPanel.add(msgTemplateMiniPagePathTextField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        templateMiniProgramOptionalLabel1 = new JLabel();
        templateMiniProgramOptionalLabel1.setText("（选填）");
        templateMsgPanel.add(templateMiniProgramOptionalLabel1, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        templateMiniProgramOptionalLabel2 = new JLabel();
        templateMiniProgramOptionalLabel2.setText("（选填）");
        templateMsgPanel.add(templateMiniProgramOptionalLabel2, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        templateKeyWordLabel = new JLabel();
        templateKeyWordLabel.setText("放大关键词");
        templateMsgPanel.add(templateKeyWordLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgTemplateKeyWordTextField = new JTextField();
        templateMsgPanel.add(msgTemplateKeyWordTextField, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        kefuMsgPanel = new JPanel();
        kefuMsgPanel.setLayout(new GridLayoutManager(6, 2, new Insets(10, 0, 0, 0), -1, -1));
        messageEditPanel.add(kefuMsgPanel, new GridConstraints(1, 2, 2, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        kefuMsgPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "客服消息编辑", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, kefuMsgPanel.getFont())));
        kefuMsgTypeLabel = new JLabel();
        kefuMsgTypeLabel.setText("消息类型");
        kefuMsgPanel.add(kefuMsgTypeLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        kefuMsgPanel.add(spacer1, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        msgKefuMsgTypeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("图文消息");
        defaultComboBoxModel1.addElement("文本消息");
        msgKefuMsgTypeComboBox.setModel(defaultComboBoxModel1);
        kefuMsgPanel.add(msgKefuMsgTypeComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        kefuMsgTitleLabel = new JLabel();
        kefuMsgTitleLabel.setText("内容/标题");
        kefuMsgPanel.add(kefuMsgTitleLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgKefuMsgTitleTextField = new JTextField();
        kefuMsgPanel.add(msgKefuMsgTitleTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(380, -1), new Dimension(380, -1), null, 0, false));
        kefuMsgPicUrlLabel = new JLabel();
        kefuMsgPicUrlLabel.setText("图片URL");
        kefuMsgPanel.add(kefuMsgPicUrlLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgKefuPicUrlTextField = new JTextField();
        kefuMsgPanel.add(msgKefuPicUrlTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        kefuMsgDescLabel = new JLabel();
        kefuMsgDescLabel.setText("描述");
        kefuMsgPanel.add(kefuMsgDescLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgKefuDescTextField = new JTextField();
        kefuMsgPanel.add(msgKefuDescTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        kefuMsgUrlLabel = new JLabel();
        kefuMsgUrlLabel.setText("跳转URL");
        kefuMsgPanel.add(kefuMsgUrlLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgKefuUrlTextField = new JTextField();
        kefuMsgPanel.add(msgKefuUrlTextField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer2 = new Spacer();
        messageEditPanel.add(spacer2, new GridConstraints(1, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        yunpianMsgPanel = new JPanel();
        yunpianMsgPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        messageEditPanel.add(yunpianMsgPanel, new GridConstraints(1, 4, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        yunpianMsgPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "云片网短信编辑", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, yunpianMsgPanel.getFont())));
        final JScrollPane scrollPane2 = new JScrollPane();
        yunpianMsgPanel.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(72, 19), null, 0, false));
        msgYunpianMsgContentTextField = new JTextArea();
        scrollPane2.setViewportView(msgYunpianMsgContentTextField);
        msgNameLabel.setLabelFor(msgNameField);
        previewMemberLabel.setLabelFor(previewUserField);
        templateIdLabel.setLabelFor(msgTemplateIdTextField);
        templateUrlLabel.setLabelFor(msgTemplateUrlTextField);
        templateMsgNameLabel.setLabelFor(templateDataNameTextField);
        templateMsgValueLabel.setLabelFor(templateDataValueTextField);
        templateMsgColorLabel.setLabelFor(templateDataColorTextField);
        templateMiniProgramAppidLabel.setLabelFor(msgTemplateMiniAppidTextField);
        templateMiniProgramPagePathLabel.setLabelFor(msgTemplateMiniPagePathTextField);
        templateKeyWordLabel.setLabelFor(msgTemplateUrlTextField);
        kefuMsgTypeLabel.setLabelFor(msgKefuMsgTypeComboBox);
        kefuMsgTitleLabel.setLabelFor(msgKefuMsgTitleTextField);
        kefuMsgPicUrlLabel.setLabelFor(msgKefuPicUrlTextField);
        kefuMsgDescLabel.setLabelFor(msgKefuDescTextField);
        kefuMsgUrlLabel.setLabelFor(msgKefuUrlTextField);
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
