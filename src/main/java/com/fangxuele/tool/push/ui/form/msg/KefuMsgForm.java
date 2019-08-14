package com.fangxuele.tool.push.ui.form.msg;

import com.fangxuele.tool.push.dao.TMsgKefuMapper;
import com.fangxuele.tool.push.domain.TMsgKefu;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Objects;

/**
 * <pre>
 * KefuMsgForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/3.
 */
@Getter
public class KefuMsgForm implements IMsgForm {
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
    private JLabel contentLabel;
    private JTextArea contentTextArea;

    private static KefuMsgForm kefuMsgForm;

    private static TMsgKefuMapper msgKefuMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuMapper.class);

    public KefuMsgForm() {
        // 客服消息类型切换事件
        msgKefuMsgTypeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                switchKefuMsgType(e.getItem().toString());
            }
        });
    }

    @Override
    public void init(String msgName) {
        clearAllField();
        List<TMsgKefu> tMsgKefuList = msgKefuMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.KEFU_CODE, msgName);
        if (tMsgKefuList.size() > 0) {
            TMsgKefu tMsgKefu = tMsgKefuList.get(0);
            String kefuMsgType = tMsgKefu.getKefuMsgType();
            getInstance().getMsgKefuMsgTypeComboBox().setSelectedItem(kefuMsgType);
            if ("文本消息".equals(kefuMsgType)) {
                getInstance().getContentTextArea().setText(tMsgKefu.getContent());
            } else if ("图文消息".equals(kefuMsgType)) {
                getInstance().getMsgKefuMsgTitleTextField().setText(tMsgKefu.getTitle());
            }
            getInstance().getMsgKefuPicUrlTextField().setText(tMsgKefu.getImgUrl());
            getInstance().getMsgKefuDescTextField().setText(tMsgKefu.getDescribe());
            getInstance().getMsgKefuUrlTextField().setText(tMsgKefu.getUrl());

            switchKefuMsgType(kefuMsgType);
        } else {
            switchKefuMsgType("图文消息");
        }
    }

    @Override
    public void save(String msgName) {
        boolean existSameMsg = false;

        List<TMsgKefu> tMsgKefuList = msgKefuMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.KEFU_CODE, msgName);
        if (tMsgKefuList.size() > 0) {
            existSameMsg = true;
        }

        int isCover = JOptionPane.NO_OPTION;
        if (existSameMsg) {
            // 如果存在，是否覆盖
            isCover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(), "已经存在同名的历史消息，\n是否覆盖？", "确认",
                    JOptionPane.YES_NO_OPTION);
        }

        if (!existSameMsg || isCover == JOptionPane.YES_OPTION) {
            String kefuMsgType = Objects.requireNonNull(getInstance().getMsgKefuMsgTypeComboBox().getSelectedItem()).toString();
            String kefuMsgContent = getInstance().getContentTextArea().getText();
            String kefuMsgTitle = getInstance().getMsgKefuMsgTitleTextField().getText();
            String kefuPicUrl = getInstance().getMsgKefuPicUrlTextField().getText();
            String kefuDesc = getInstance().getMsgKefuDescTextField().getText();
            String kefuUrl = getInstance().getMsgKefuUrlTextField().getText();

            String now = SqliteUtil.nowDateForSqlite();

            TMsgKefu tMsgKefu = new TMsgKefu();
            tMsgKefu.setMsgType(MessageTypeEnum.KEFU_CODE);
            tMsgKefu.setMsgName(msgName);
            tMsgKefu.setKefuMsgType(kefuMsgType);
            tMsgKefu.setContent(kefuMsgContent);
            tMsgKefu.setTitle(kefuMsgTitle);
            tMsgKefu.setImgUrl(kefuPicUrl);
            tMsgKefu.setDescribe(kefuDesc);
            tMsgKefu.setUrl(kefuUrl);
            tMsgKefu.setModifiedTime(now);

            if (existSameMsg) {
                msgKefuMapper.updateByMsgTypeAndMsgName(tMsgKefu);
            } else {
                tMsgKefu.setCreateTime(now);
                msgKefuMapper.insertSelective(tMsgKefu);
            }

            JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "保存成功！", "成功",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static KefuMsgForm getInstance() {
        if (kefuMsgForm == null) {
            kefuMsgForm = new KefuMsgForm();
        }
        return kefuMsgForm;
    }

    /**
     * 根据客服消息类型转换界面显示
     *
     * @param msgType 消息类型
     */
    public static void switchKefuMsgType(String msgType) {
        switch (msgType) {
            case "文本消息":
                getInstance().getContentTextArea().setVisible(true);
                getInstance().getKefuMsgDescLabel().setVisible(false);
                getInstance().getMsgKefuDescTextField().setVisible(false);
                getInstance().getKefuMsgPicUrlLabel().setVisible(false);
                getInstance().getMsgKefuPicUrlTextField().setVisible(false);
                getInstance().getKefuMsgUrlLabel().setVisible(false);
                getInstance().getMsgKefuUrlTextField().setVisible(false);
                getInstance().getKefuMsgTitleLabel().setVisible(false);
                getInstance().getMsgKefuMsgTitleTextField().setVisible(false);
                break;
            case "图文消息":
                getInstance().getContentLabel().setVisible(false);
                getInstance().getContentTextArea().setVisible(false);
                getInstance().getKefuMsgDescLabel().setVisible(true);
                getInstance().getMsgKefuDescTextField().setVisible(true);
                getInstance().getKefuMsgPicUrlLabel().setVisible(true);
                getInstance().getMsgKefuPicUrlTextField().setVisible(true);
                getInstance().getKefuMsgUrlLabel().setVisible(true);
                getInstance().getMsgKefuUrlTextField().setVisible(true);
                getInstance().getKefuMsgTitleLabel().setVisible(true);
                getInstance().getMsgKefuMsgTitleTextField().setVisible(true);
                break;
            default:
                break;
        }
    }

    /**
     * 清空所有界面字段
     */
    public static void clearAllField() {
        getInstance().getContentTextArea().setText("");
        getInstance().getMsgKefuMsgTitleTextField().setText("");
        getInstance().getMsgKefuPicUrlTextField().setText("");
        getInstance().getMsgKefuDescTextField().setText("");
        getInstance().getMsgKefuUrlTextField().setText("");
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
        kefuMsgPanel.setLayout(new GridLayoutManager(7, 2, new Insets(10, 15, 0, 0), -1, -1));
        panel1.add(kefuMsgPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        kefuMsgPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "客服消息编辑", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, kefuMsgPanel.getFont())));
        kefuMsgTypeLabel = new JLabel();
        kefuMsgTypeLabel.setText("消息类型");
        kefuMsgPanel.add(kefuMsgTypeLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        kefuMsgPanel.add(spacer1, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        msgKefuMsgTypeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("图文消息");
        defaultComboBoxModel1.addElement("文本消息");
        msgKefuMsgTypeComboBox.setModel(defaultComboBoxModel1);
        kefuMsgPanel.add(msgKefuMsgTypeComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        kefuMsgTitleLabel = new JLabel();
        kefuMsgTitleLabel.setText("标题");
        kefuMsgPanel.add(kefuMsgTitleLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgKefuMsgTitleTextField = new JTextField();
        kefuMsgPanel.add(msgKefuMsgTitleTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(380, -1), new Dimension(380, -1), null, 0, false));
        kefuMsgPicUrlLabel = new JLabel();
        kefuMsgPicUrlLabel.setText("图片URL");
        kefuMsgPanel.add(kefuMsgPicUrlLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgKefuPicUrlTextField = new JTextField();
        kefuMsgPanel.add(msgKefuPicUrlTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        kefuMsgDescLabel = new JLabel();
        kefuMsgDescLabel.setText("描述");
        kefuMsgPanel.add(kefuMsgDescLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgKefuDescTextField = new JTextField();
        kefuMsgPanel.add(msgKefuDescTextField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        kefuMsgUrlLabel = new JLabel();
        kefuMsgUrlLabel.setText("跳转URL");
        kefuMsgPanel.add(kefuMsgUrlLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgKefuUrlTextField = new JTextField();
        kefuMsgPanel.add(msgKefuUrlTextField, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        contentLabel = new JLabel();
        contentLabel.setText("内容");
        kefuMsgPanel.add(contentLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        contentTextArea = new JTextArea();
        kefuMsgPanel.add(contentTextArea, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
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
