package com.fangxuele.tool.push.ui.form.msg;

import com.fangxuele.tool.push.dao.TDingAppMapper;
import com.fangxuele.tool.push.dao.TMsgWxCpMapper;
import com.fangxuele.tool.push.domain.TDingApp;
import com.fangxuele.tool.push.domain.TMsgWxCp;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.dialog.DingAppDialog;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.google.common.collect.Maps;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <pre>
 * DingMsgForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/9/4.
 */
@Getter
public class DingMsgForm implements IMsgForm {
    private JPanel dingMsgPanel;
    private JLabel msgTypeLabel;
    private JComboBox msgTypeComboBox;
    private JLabel titleLabel;
    private JLabel picUrlLabel;
    private JTextField picUrlTextField;
    private JLabel descLabel;
    private JTextField descTextField;
    private JLabel urlLabel;
    private JTextField urlTextField;
    private JLabel contentLabel;
    private JComboBox appNameComboBox;
    private JButton appManageButton;
    private JTextField titleTextField;
    private JTextArea contentTextArea;
    private JLabel btnTxtLabel;
    private JTextField btnTxtTextField;

    private static DingMsgForm dingMsgForm;

    private static TMsgWxCpMapper msgWxCpMapper = MybatisUtil.getSqlSession().getMapper(TMsgWxCpMapper.class);
    private static TDingAppMapper dingAppMapper = MybatisUtil.getSqlSession().getMapper(TDingAppMapper.class);

    public static Map<String, String> appNameToAgentIdMap = Maps.newHashMap();
    public static Map<String, String> agentIdToAppNameMap = Maps.newHashMap();

    public DingMsgForm() {
        // 消息类型切换事件
        msgTypeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                switchCpMsgType(e.getItem().toString());
            }
        });
        appManageButton.addActionListener(e -> {
            DingAppDialog dialog = new DingAppDialog();
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

    public static DingMsgForm getInstance() {
        if (dingMsgForm == null) {
            dingMsgForm = new DingMsgForm();
        }
        return dingMsgForm;
    }

    /**
     * 初始化应用名称列表
     */
    public static void initAppNameList() {
        List<TDingApp> tDingAppList = dingAppMapper.selectAll();
        getInstance().getAppNameComboBox().removeAllItems();
        for (TDingApp tDingApp : tDingAppList) {
            appNameToAgentIdMap.put(tDingApp.getAppName(), tDingApp.getAgentId());
            agentIdToAppNameMap.put(tDingApp.getAgentId(), tDingApp.getAppName());
            getInstance().getAppNameComboBox().addItem(tDingApp.getAppName());
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
        panel1.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        dingMsgPanel = new JPanel();
        dingMsgPanel.setLayout(new GridLayoutManager(9, 3, new Insets(10, 8, 0, 8), -1, -1));
        panel1.add(dingMsgPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        dingMsgPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, dingMsgPanel.getFont())));
        msgTypeLabel = new JLabel();
        msgTypeLabel.setText("消息类型");
        dingMsgPanel.add(msgTypeLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        dingMsgPanel.add(spacer1, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        msgTypeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("文本消息");
        defaultComboBoxModel1.addElement("链接消息");
        defaultComboBoxModel1.addElement("markdown消息");
        defaultComboBoxModel1.addElement("卡片消息");
        msgTypeComboBox.setModel(defaultComboBoxModel1);
        dingMsgPanel.add(msgTypeComboBox, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        titleLabel = new JLabel();
        titleLabel.setText("标题");
        dingMsgPanel.add(titleLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        picUrlLabel = new JLabel();
        picUrlLabel.setText("图片URL");
        dingMsgPanel.add(picUrlLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        picUrlTextField = new JTextField();
        dingMsgPanel.add(picUrlTextField, new GridConstraints(4, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        descLabel = new JLabel();
        descLabel.setText("描述");
        dingMsgPanel.add(descLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        descTextField = new JTextField();
        dingMsgPanel.add(descTextField, new GridConstraints(5, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        urlLabel = new JLabel();
        urlLabel.setText("跳转URL");
        dingMsgPanel.add(urlLabel, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        urlTextField = new JTextField();
        dingMsgPanel.add(urlTextField, new GridConstraints(6, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        contentLabel = new JLabel();
        contentLabel.setText("内容");
        dingMsgPanel.add(contentLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("选择应用");
        dingMsgPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        appNameComboBox = new JComboBox();
        dingMsgPanel.add(appNameComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        appManageButton = new JButton();
        appManageButton.setText("应用管理");
        dingMsgPanel.add(appManageButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        titleTextField = new JTextField();
        dingMsgPanel.add(titleTextField, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(380, -1), new Dimension(380, -1), null, 0, false));
        contentTextArea = new JTextArea();
        dingMsgPanel.add(contentTextArea, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        btnTxtLabel = new JLabel();
        btnTxtLabel.setText("按钮文字");
        btnTxtLabel.setToolTipText("可不填。默认为“详情”， 不超过4个文字，超过自动截断");
        dingMsgPanel.add(btnTxtLabel, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnTxtTextField = new JTextField();
        btnTxtTextField.setToolTipText("可不填。默认为“详情”， 不超过4个文字，超过自动截断");
        dingMsgPanel.add(btnTxtTextField, new GridConstraints(7, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel1.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel1.add(spacer3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
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
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

}
