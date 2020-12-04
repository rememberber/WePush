package com.fangxuele.tool.push.ui.form.msg;

import com.fangxuele.tool.push.dao.TMsgWxCpMapper;
import com.fangxuele.tool.push.dao.TWxCpAppMapper;
import com.fangxuele.tool.push.domain.TMsgWxCp;
import com.fangxuele.tool.push.domain.TWxCpApp;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.dialog.WxCpAppDialog;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.google.common.collect.Maps;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * <pre>
 * 微信企业号/企业微信消息form
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/29.
 */
@Getter
public class WxCpMsgForm implements IMsgForm {
    private JPanel wxCpMsgPanel;
    private JLabel msgTypeLabel;
    private JComboBox msgTypeComboBox;
    private JLabel titleLabel;
    private JTextField titleTextField;
    private JLabel picUrlLabel;
    private JTextField picUrlTextField;
    private JLabel descLabel;
    private JTextField descTextField;
    private JLabel urlLabel;
    private JTextField urlTextField;
    private JLabel contentLabel;
    private JComboBox appNameComboBox;
    private JButton appManageButton;
    private JTextArea contentTextArea;
    private JTextField btnTxtTextField;
    private JLabel btnTxtLabel;

    private static WxCpMsgForm wxCpMsgForm;

    private static TMsgWxCpMapper msgWxCpMapper = MybatisUtil.getSqlSession().getMapper(TMsgWxCpMapper.class);
    private static TWxCpAppMapper wxCpAppMapper = MybatisUtil.getSqlSession().getMapper(TWxCpAppMapper.class);

    public static Map<String, String> appNameToAgentIdMap = Maps.newHashMap();
    public static Map<String, String> agentIdToAppNameMap = Maps.newHashMap();

    public WxCpMsgForm() {
        // 消息类型切换事件
        msgTypeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                switchCpMsgType(e.getItem().toString());
            }
        });
        appManageButton.addActionListener(e -> {
            WxCpAppDialog dialog = new WxCpAppDialog();
            dialog.renderTable();
            dialog.pack();
            dialog.setVisible(true);
            initAppNameList();
        });
    }

    @Override
    public void init(String msgName) {
        clearAllField();
        initAppNameList();
        List<TMsgWxCp> tMsgWxCpList = msgWxCpMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.WX_CP_CODE, msgName);
        if (tMsgWxCpList.size() > 0) {
            TMsgWxCp tMsgWxCp = tMsgWxCpList.get(0);
            String cpMsgType = tMsgWxCp.getCpMsgType();
            getInstance().getAppNameComboBox().setSelectedItem(agentIdToAppNameMap.get(tMsgWxCp.getAgentId()));
            getInstance().getMsgTypeComboBox().setSelectedItem(cpMsgType);
            getInstance().getContentTextArea().setText(tMsgWxCp.getContent());
            getInstance().getTitleTextField().setText(tMsgWxCp.getTitle());
            getInstance().getPicUrlTextField().setText(tMsgWxCp.getImgUrl());
            getInstance().getDescTextField().setText(tMsgWxCp.getDescribe());
            getInstance().getUrlTextField().setText(tMsgWxCp.getUrl());
            getInstance().getBtnTxtTextField().setText(tMsgWxCp.getBtnTxt());

            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            messageEditForm.getMsgNameField().setText(tMsgWxCp.getMsgName());
            messageEditForm.getPreviewUserField().setText(tMsgWxCp.getPreviewUser());

            switchCpMsgType(cpMsgType);
        } else {
            switchCpMsgType("图文消息");
        }
    }

    @Override
    public void save(String msgName) {
        boolean existSameMsg = false;

        if (getInstance().getAppNameComboBox().getSelectedItem() == null) {
            JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "请选择应用！", "成功",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<TMsgWxCp> tMsgWxCpList = msgWxCpMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.WX_CP_CODE, msgName);
        if (tMsgWxCpList.size() > 0) {
            existSameMsg = true;
        }

        int isCover = JOptionPane.NO_OPTION;
        if (existSameMsg) {
            // 如果存在，是否覆盖
            isCover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(), "已经存在同名的历史消息，\n是否覆盖？", "确认",
                    JOptionPane.YES_NO_OPTION);
        }

        if (!existSameMsg || isCover == JOptionPane.YES_OPTION) {
            String cpMsgType = Objects.requireNonNull(getInstance().getMsgTypeComboBox().getSelectedItem()).toString();
            String content = getInstance().getContentTextArea().getText();
            String title = getInstance().getTitleTextField().getText();
            String picUrl = getInstance().getPicUrlTextField().getText();
            String desc = getInstance().getDescTextField().getText();
            String url = getInstance().getUrlTextField().getText();
            String btnTxt = getInstance().getBtnTxtTextField().getText();

            String now = SqliteUtil.nowDateForSqlite();

            TMsgWxCp tMsgWxCp = new TMsgWxCp();
            tMsgWxCp.setMsgType(MessageTypeEnum.WX_CP_CODE);
            tMsgWxCp.setMsgName(msgName);
            tMsgWxCp.setAgentId(appNameToAgentIdMap.get(getInstance().getAppNameComboBox().getSelectedItem()));
            tMsgWxCp.setCpMsgType(cpMsgType);
            tMsgWxCp.setContent(content);
            tMsgWxCp.setTitle(title);
            tMsgWxCp.setImgUrl(picUrl);
            tMsgWxCp.setDescribe(desc);
            tMsgWxCp.setUrl(url);
            tMsgWxCp.setBtnTxt(btnTxt);
            tMsgWxCp.setModifiedTime(now);

            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            tMsgWxCp.setPreviewUser(messageEditForm.getPreviewUserField().getText());

            if (existSameMsg) {
                msgWxCpMapper.updateByMsgTypeAndMsgName(tMsgWxCp);
            } else {
                tMsgWxCp.setCreateTime(now);
                msgWxCpMapper.insertSelective(tMsgWxCp);
            }

            JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "保存成功！", "成功",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static WxCpMsgForm getInstance() {
        if (wxCpMsgForm == null) {
            wxCpMsgForm = new WxCpMsgForm();
        }
        return wxCpMsgForm;
    }

    /**
     * 初始化应用名称列表
     */
    public static void initAppNameList() {
        List<TWxCpApp> tWxCpAppList = wxCpAppMapper.selectAll();
        getInstance().getAppNameComboBox().removeAllItems();
        for (TWxCpApp tWxCpApp : tWxCpAppList) {
            appNameToAgentIdMap.put(tWxCpApp.getAppName(), tWxCpApp.getAgentId());
            agentIdToAppNameMap.put(tWxCpApp.getAgentId(), tWxCpApp.getAppName());
            getInstance().getAppNameComboBox().addItem(tWxCpApp.getAppName());
        }
    }

    /**
     * 根据消息类型转换界面显示
     *
     * @param msgType 消息类型
     */
    public static void switchCpMsgType(String msgType) {
        switch (msgType) {
            case "文本消息":
            case "markdown消息":
                getInstance().getContentTextArea().setVisible(true);
                getInstance().getDescLabel().setVisible(false);
                getInstance().getDescTextField().setVisible(false);
                getInstance().getPicUrlLabel().setVisible(false);
                getInstance().getPicUrlTextField().setVisible(false);
                getInstance().getUrlLabel().setVisible(false);
                getInstance().getUrlTextField().setVisible(false);
                getInstance().getTitleLabel().setVisible(false);
                getInstance().getTitleTextField().setVisible(false);
                getInstance().getBtnTxtLabel().setVisible(false);
                getInstance().getBtnTxtTextField().setVisible(false);
                break;
            case "图文消息":
                getInstance().getContentLabel().setVisible(false);
                getInstance().getContentTextArea().setVisible(false);
                getInstance().getBtnTxtLabel().setVisible(false);
                getInstance().getBtnTxtTextField().setVisible(false);
                getInstance().getDescLabel().setVisible(true);
                getInstance().getDescTextField().setVisible(true);
                getInstance().getPicUrlLabel().setVisible(true);
                getInstance().getPicUrlTextField().setVisible(true);
                getInstance().getUrlLabel().setVisible(true);
                getInstance().getUrlTextField().setVisible(true);
                getInstance().getTitleLabel().setVisible(true);
                getInstance().getTitleTextField().setVisible(true);
                break;
            case "文本卡片消息":
                getInstance().getContentLabel().setVisible(false);
                getInstance().getContentTextArea().setVisible(false);
                getInstance().getPicUrlLabel().setVisible(false);
                getInstance().getPicUrlTextField().setVisible(false);
                getInstance().getDescLabel().setVisible(true);
                getInstance().getDescTextField().setVisible(true);
                getInstance().getBtnTxtLabel().setVisible(true);
                getInstance().getBtnTxtTextField().setVisible(true);
                getInstance().getUrlLabel().setVisible(true);
                getInstance().getUrlTextField().setVisible(true);
                getInstance().getTitleLabel().setVisible(true);
                getInstance().getTitleTextField().setVisible(true);
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
        getInstance().getTitleTextField().setText("");
        getInstance().getPicUrlTextField().setText("");
        getInstance().getDescTextField().setText("");
        getInstance().getUrlTextField().setText("");
        getInstance().getBtnTxtTextField().setText("");
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
        wxCpMsgPanel = new JPanel();
        wxCpMsgPanel.setLayout(new GridLayoutManager(9, 3, new Insets(10, 8, 0, 8), -1, -1));
        panel1.add(wxCpMsgPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        wxCpMsgPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, wxCpMsgPanel.getFont()), null));
        msgTypeLabel = new JLabel();
        msgTypeLabel.setText("消息类型");
        wxCpMsgPanel.add(msgTypeLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        wxCpMsgPanel.add(spacer1, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        msgTypeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("图文消息");
        defaultComboBoxModel1.addElement("文本消息");
        defaultComboBoxModel1.addElement("文本卡片消息");
        defaultComboBoxModel1.addElement("markdown消息");
        msgTypeComboBox.setModel(defaultComboBoxModel1);
        wxCpMsgPanel.add(msgTypeComboBox, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        titleLabel = new JLabel();
        titleLabel.setText("标题");
        wxCpMsgPanel.add(titleLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        picUrlLabel = new JLabel();
        picUrlLabel.setText("图片URL");
        wxCpMsgPanel.add(picUrlLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        picUrlTextField = new JTextField();
        wxCpMsgPanel.add(picUrlTextField, new GridConstraints(4, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        descLabel = new JLabel();
        descLabel.setText("描述");
        wxCpMsgPanel.add(descLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        descTextField = new JTextField();
        wxCpMsgPanel.add(descTextField, new GridConstraints(5, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        urlLabel = new JLabel();
        urlLabel.setText("跳转URL");
        wxCpMsgPanel.add(urlLabel, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        urlTextField = new JTextField();
        wxCpMsgPanel.add(urlTextField, new GridConstraints(6, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        contentLabel = new JLabel();
        contentLabel.setText("内容");
        wxCpMsgPanel.add(contentLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("选择应用");
        wxCpMsgPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        appNameComboBox = new JComboBox();
        wxCpMsgPanel.add(appNameComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        appManageButton = new JButton();
        appManageButton.setText("应用管理");
        wxCpMsgPanel.add(appManageButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        titleTextField = new JTextField();
        wxCpMsgPanel.add(titleTextField, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(380, -1), new Dimension(380, -1), null, 0, false));
        contentTextArea = new JTextArea();
        wxCpMsgPanel.add(contentTextArea, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        btnTxtLabel = new JLabel();
        btnTxtLabel.setText("按钮文字");
        btnTxtLabel.setToolTipText("可不填。默认为“详情”， 不超过4个文字，超过自动截断");
        wxCpMsgPanel.add(btnTxtLabel, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnTxtTextField = new JTextField();
        btnTxtTextField.setToolTipText("可不填。默认为“详情”， 不超过4个文字，超过自动截断");
        wxCpMsgPanel.add(btnTxtTextField, new GridConstraints(7, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        msgTypeLabel.setLabelFor(msgTypeComboBox);
        titleLabel.setLabelFor(titleTextField);
        picUrlLabel.setLabelFor(picUrlTextField);
        descLabel.setLabelFor(descTextField);
        urlLabel.setLabelFor(urlTextField);
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
