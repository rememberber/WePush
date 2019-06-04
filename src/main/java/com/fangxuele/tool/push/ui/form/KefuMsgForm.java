package com.fangxuele.tool.push.ui.form;

import com.fangxuele.tool.push.dao.TMsgKefuMapper;
import com.fangxuele.tool.push.domain.TMsgKefu;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * <pre>
 * 类说明
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/3.
 */
@Getter
public class KefuMsgForm {
    private JPanel kefuMsgPanel;
    private JLabel kefuMsgTypeLabel;
    private JComboBox msgKefuMsgTypeComboBox;
    private JLabel kefuMsgTitleLabel;
    private JTextField msgKefuMsgTitleTextField;
    private JLabel kefuMsgPicUrlLabel;
    private JTextField msgKefuPicUrlTextField;
    private JLabel kefuMsgDescLabel;
    private JTextField msgKefuDescTextField;
    private JLabel kefuMsgUrlLabel;
    private JTextField msgKefuUrlTextField;

    public static KefuMsgForm kefuMsgForm = new KefuMsgForm();

    private static TMsgKefuMapper msgKefuMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuMapper.class);

    public KefuMsgForm() {
        // 客服消息类型切换事件
        kefuMsgForm.getMsgKefuMsgTypeComboBox().addItemListener(e -> KefuMsgForm.switchKefuMsgType(e.getItem().toString()));
    }

    public static void init(String msgName) {
        clearAllField();
        List<TMsgKefu> tMsgKefuList = msgKefuMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.KEFU_CODE, msgName);
        if (tMsgKefuList.size() > 0) {
            TMsgKefu tMsgKefu = tMsgKefuList.get(0);
            String kefuMsgType = tMsgKefu.getKefuMsgType();
            kefuMsgForm.getMsgKefuMsgTypeComboBox().setSelectedItem(kefuMsgType);
            if ("文本消息".equals(kefuMsgType)) {
                kefuMsgForm.getMsgKefuMsgTitleTextField().setText(tMsgKefu.getContent());
            } else if ("图文消息".equals(kefuMsgType)) {
                kefuMsgForm.getMsgKefuMsgTitleTextField().setText(tMsgKefu.getTitle());
            }
            kefuMsgForm.getMsgKefuPicUrlTextField().setText(tMsgKefu.getImgUrl());
            kefuMsgForm.getMsgKefuDescTextField().setText(tMsgKefu.getDescribe());
            kefuMsgForm.getMsgKefuUrlTextField().setText(tMsgKefu.getUrl());

            switchKefuMsgType(kefuMsgType);
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
                kefuMsgForm.getKefuMsgTitleLabel().setText("内容");
                kefuMsgForm.getKefuMsgDescLabel().setVisible(false);
                kefuMsgForm.getMsgKefuDescTextField().setVisible(false);
                kefuMsgForm.getKefuMsgPicUrlLabel().setVisible(false);
                kefuMsgForm.getMsgKefuPicUrlTextField().setVisible(false);
                kefuMsgForm.getMsgKefuDescTextField().setVisible(false);
                kefuMsgForm.getKefuMsgUrlLabel().setVisible(false);
                kefuMsgForm.getMsgKefuUrlTextField().setVisible(false);
                break;
            case "图文消息":
                kefuMsgForm.getKefuMsgTitleLabel().setText("标题");
                kefuMsgForm.getKefuMsgDescLabel().setVisible(true);
                kefuMsgForm.getMsgKefuDescTextField().setVisible(true);
                kefuMsgForm.getKefuMsgPicUrlLabel().setVisible(true);
                kefuMsgForm.getMsgKefuPicUrlTextField().setVisible(true);
                kefuMsgForm.getMsgKefuDescTextField().setVisible(true);
                kefuMsgForm.getKefuMsgUrlLabel().setVisible(true);
                kefuMsgForm.getMsgKefuUrlTextField().setVisible(true);
                break;
            default:
                break;
        }
    }

    /**
     * 清空所有界面字段
     */
    public static void clearAllField() {
        // TODO
//        messageEditForm.getMsgNameField().setText("");
//        messageEditForm.getMsgTemplateIdTextField().setText("");
//        messageEditForm.getMsgTemplateUrlTextField().setText("");
//        messageEditForm.getMsgKefuMsgTitleTextField().setText("");
//        messageEditForm.getMsgKefuPicUrlTextField().setText("");
//        messageEditForm.getMsgKefuDescTextField().setText("");
//        messageEditForm.getMsgKefuUrlTextField().setText("");
//        messageEditForm.getMsgTemplateMiniAppidTextField().setText("");
//        messageEditForm.getMsgTemplateMiniPagePathTextField().setText("");
//        messageEditForm.getMsgTemplateKeyWordTextField().setText("");
//        messageEditForm.getMsgYunpianMsgContentTextField().setText("");
//        messageEditForm.getTemplateDataNameTextField().setText("");
//        messageEditForm.getTemplateDataValueTextField().setText("");
//        messageEditForm.getTemplateDataColorTextField().setText("");
//        messageEditForm.getPreviewUserField().setText("");
    }

    public static void save(String msgName) {
        int msgId = 0;
        boolean existSameMsg = false;

        List<TMsgKefu> tMsgKefuList = msgKefuMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.KEFU_CODE, msgName);
        if (tMsgKefuList.size() > 0) {
            existSameMsg = true;
            msgId = tMsgKefuList.get(0).getId();
        }

        int isCover = JOptionPane.NO_OPTION;
        if (existSameMsg) {
            // 如果存在，是否覆盖
            isCover = JOptionPane.showConfirmDialog(MessageEditForm.messageEditForm.getMsgEditorPanel(), "已经存在同名的历史消息，\n是否覆盖？", "确认",
                    JOptionPane.YES_NO_OPTION);
        }

        if (!existSameMsg || isCover == JOptionPane.YES_OPTION) {
            String kefuMsgType = Objects.requireNonNull(kefuMsgForm.getMsgKefuMsgTypeComboBox().getSelectedItem()).toString();
            String kefuMsgTitle = kefuMsgForm.getMsgKefuMsgTitleTextField().getText();
            String kefuPicUrl = kefuMsgForm.getMsgKefuPicUrlTextField().getText();
            String kefuDesc = kefuMsgForm.getMsgKefuDescTextField().getText();
            String kefuUrl = kefuMsgForm.getMsgKefuUrlTextField().getText();

            String now = SqliteUtil.nowDateForSqlite();

            TMsgKefu tMsgKefu = new TMsgKefu();
            tMsgKefu.setMsgType(MessageTypeEnum.KEFU_CODE);
            tMsgKefu.setMsgName(msgName);
            tMsgKefu.setKefuMsgType(kefuMsgType);
            tMsgKefu.setContent(kefuMsgTitle);
            tMsgKefu.setTitle(kefuMsgTitle);
            tMsgKefu.setImgUrl(kefuPicUrl);
            tMsgKefu.setDescribe(kefuDesc);
            tMsgKefu.setUrl(kefuUrl);
            tMsgKefu.setCreateTime(now);
            tMsgKefu.setModifiedTime(now);

            if (existSameMsg) {
                msgKefuMapper.updateByMsgTypeAndMsgName(tMsgKefu);
            } else {
                msgKefuMapper.insertSelective(tMsgKefu);
                msgId = tMsgKefu.getId();
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
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        kefuMsgPanel = new JPanel();
        kefuMsgPanel.setLayout(new GridLayoutManager(6, 2, new Insets(10, 15, 0, 0), -1, -1));
        panel1.add(kefuMsgPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        kefuMsgPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "客服消息编辑", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, kefuMsgPanel.getFont())));
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
