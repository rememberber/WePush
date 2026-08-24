package com.fangxuele.tool.push.ui.form.msg;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgWxCp;
import com.fangxuele.tool.push.domain.WxCpMiniProgramContentItem;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.fangxuele.tool.push.util.WxCpMiniProgramNoticeSupport;
import com.google.common.collect.Maps;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
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
    private JTextArea descTextField;
    private JLabel urlLabel;
    private JTextField urlTextField;
    private JLabel contentLabel;
    private JButton appManageButton;
    private JTextArea contentTextArea;
    private JTextField btnTxtTextField;
    private JLabel btnTxtLabel;
    private JPanel miniProgramPanel;
    private JTextField miniProgramAppIdTextField;
    private JTextField miniProgramPageTextField;
    private JTextField miniProgramTitleTextField;
    private JTextField miniProgramDescriptionTextField;
    private JTable miniProgramContentTable;
    private DefaultTableModel miniProgramContentTableModel;
    private JCheckBox miniProgramEmphasisFirstItemCheckBox;
    private JCheckBox enableIdTransCheckBox;
    private JCheckBox enableDuplicateCheckCheckBox;
    private JSpinner duplicateCheckIntervalSpinner;

    private static WxCpMsgForm wxCpMsgForm;

    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    public static Map<String, String> appNameToAgentIdMap = Maps.newHashMap();
    public static Map<String, String> agentIdToAppNameMap = Maps.newHashMap();

    public WxCpMsgForm() {
        initMiniProgramPanel();
        // 消息类型切换事件
        msgTypeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                switchCpMsgType(e.getItem().toString());
            }
        });
    }

    @Override
    public void init(Integer msgId) {
        clearAllField();
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        if (tMsg != null) {
            TMsgWxCp tMsgWxCp = JSONUtil.toBean(tMsg.getContent(), TMsgWxCp.class);
            String cpMsgType = tMsgWxCp.getCpMsgType();
            getInstance().getMsgTypeComboBox().setSelectedItem(cpMsgType);
            getInstance().getContentTextArea().setText(tMsgWxCp.getContent());
            getInstance().getTitleTextField().setText(tMsgWxCp.getTitle());
            getInstance().getPicUrlTextField().setText(tMsgWxCp.getImgUrl());
            getInstance().getDescTextField().setText(tMsgWxCp.getDescribe());
            getInstance().getUrlTextField().setText(tMsgWxCp.getUrl());
            getInstance().getBtnTxtTextField().setText(tMsgWxCp.getBtnTxt());
            loadMiniProgramFields(tMsgWxCp);

            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            messageEditForm.getMsgNameField().setText(tMsg.getMsgName());
            messageEditForm.getPreviewUserField().setText(tMsg.getPreviewUser());

            switchCpMsgType(cpMsgType);
        } else {
            switchCpMsgType("图文消息");
        }
    }

    @Override
    public void save(Integer accountId, String msgName) {
        boolean existSameMsg = false;

        Integer msgId = null;
        TMsg tMsg = msgMapper.selectByUnique(MessageTypeEnum.WX_CP_CODE, accountId, msgName);
        if (tMsg != null) {
            existSameMsg = true;
            msgId = tMsg.getId();
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

            List<WxCpMiniProgramContentItem> miniProgramContentItems = collectMiniProgramContentItems();
            if (WxCpMiniProgramNoticeSupport.MESSAGE_TYPE.equals(cpMsgType)) {
                validateMiniProgramTemplate(miniProgramContentItems);
            }

            String now = SqliteUtil.nowDateForSqlite();

            TMsg msg = new TMsg();
            TMsgWxCp tMsgWxCp = new TMsgWxCp();
            msg.setMsgType(MessageTypeEnum.WX_CP_CODE);
            msg.setAccountId(accountId);
            msg.setMsgName(msgName);
            tMsgWxCp.setCpMsgType(cpMsgType);
            tMsgWxCp.setContent(content);
            tMsgWxCp.setTitle(title);
            tMsgWxCp.setImgUrl(picUrl);
            tMsgWxCp.setDescribe(desc);
            tMsgWxCp.setUrl(url);
            tMsgWxCp.setBtnTxt(btnTxt);
            if (WxCpMiniProgramNoticeSupport.MESSAGE_TYPE.equals(cpMsgType)) {
                tMsgWxCp.setMiniProgramAppId(miniProgramAppIdTextField.getText());
                tMsgWxCp.setMiniProgramPage(miniProgramPageTextField.getText());
                tMsgWxCp.setTitle(miniProgramTitleTextField.getText());
                tMsgWxCp.setDescribe(miniProgramDescriptionTextField.getText());
                tMsgWxCp.setMiniProgramEmphasisFirstItem(miniProgramEmphasisFirstItemCheckBox.isSelected());
                tMsgWxCp.setMiniProgramContentItems(miniProgramContentItems);
                tMsgWxCp.setEnableIdTrans(enableIdTransCheckBox.isSelected());
                tMsgWxCp.setEnableDuplicateCheck(enableDuplicateCheckCheckBox.isSelected());
                tMsgWxCp.setDuplicateCheckInterval((Integer) duplicateCheckIntervalSpinner.getValue());
            }
            msg.setModifiedTime(now);

            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            msg.setPreviewUser(messageEditForm.getPreviewUserField().getText());

            msg.setContent(JSONUtil.toJsonStr(tMsgWxCp));
            if (existSameMsg) {
                msg.setId(msgId);
                msgMapper.updateByPrimaryKeySelective(msg);
            } else {
                msg.setCreateTime(now);
                msgMapper.insertSelective(msg);
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
     * 根据消息类型转换界面显示
     *
     * @param msgType 消息类型
     */
    public static void switchCpMsgType(String msgType) {
        boolean miniProgramNotice = WxCpMiniProgramNoticeSupport.MESSAGE_TYPE.equals(msgType);
        getInstance().getMiniProgramPanel().setVisible(miniProgramNotice);
        switch (msgType) {
            case "文本消息":
            case "markdown消息":
                getInstance().getContentLabel().setVisible(true);
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
            case WxCpMiniProgramNoticeSupport.MESSAGE_TYPE:
                getInstance().getContentLabel().setVisible(false);
                getInstance().getContentTextArea().setVisible(false);
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
            default:
                break;
        }
        getInstance().getWxCpMsgPanel().revalidate();
        getInstance().getWxCpMsgPanel().repaint();
    }

    /**
     * 清空所有界面字段
     */
    @Override
    public void clearAllField() {
        getInstance().getContentTextArea().setText("");
        getInstance().getTitleTextField().setText("");
        getInstance().getPicUrlTextField().setText("");
        getInstance().getDescTextField().setText("");
        getInstance().getUrlTextField().setText("");
        getInstance().getBtnTxtTextField().setText("");
        clearMiniProgramFields();
        switchCpMsgType(msgTypeComboBox.getSelectedItem().toString());
    }

    private void initMiniProgramPanel() {
        miniProgramPanel.setLayout(new GridBagLayout());
        GridBagConstraints label = miniConstraints(0, 0, 0, 0, GridBagConstraints.NONE);
        label.anchor = GridBagConstraints.NORTHWEST;
        label.insets = new Insets(4, 0, 4, 10);
        GridBagConstraints field = miniConstraints(1, 0, 1, 0, GridBagConstraints.HORIZONTAL);
        field.insets = new Insets(4, 0, 4, 0);

        miniProgramAppIdTextField = new JTextField();
        miniProgramAppIdTextField.setToolTipText("必须是与当前企业微信应用关联的小程序 AppId");
        miniProgramPanel.add(new JLabel("小程序 AppId *"), label);
        miniProgramPanel.add(miniProgramAppIdTextField, field);

        label.gridy = 1;
        field.gridy = 1;
        miniProgramPageTextField = new JTextField();
        miniProgramPageTextField.setToolTipText("可选，仅限小程序内页面，最长 1024 字节；支持变量");
        miniProgramPanel.add(new JLabel("页面路径"), label);
        miniProgramPanel.add(miniProgramPageTextField, field);

        label.gridy = 2;
        field.gridy = 2;
        miniProgramTitleTextField = new JTextField();
        miniProgramTitleTextField.setToolTipText("4-12 个字符；支持变量");
        miniProgramPanel.add(new JLabel("标题 *"), label);
        miniProgramPanel.add(miniProgramTitleTextField, field);

        label.gridy = 3;
        field.gridy = 3;
        miniProgramDescriptionTextField = new JTextField();
        miniProgramDescriptionTextField.setToolTipText("可选；填写时为 4-12 个字符，支持变量");
        miniProgramPanel.add(new JLabel("描述"), label);
        miniProgramPanel.add(miniProgramDescriptionTextField, field);

        label.gridy = 4;
        miniProgramPanel.add(new JLabel("内容项"), label);
        miniProgramContentTableModel = new DefaultTableModel(new Object[]{"键（最多10字符）", "值（最多30字符）"}, 0);
        miniProgramContentTable = new JTable(miniProgramContentTableModel);
        miniProgramContentTable.setFillsViewportHeight(true);
        JScrollPane contentScrollPane = new JScrollPane(miniProgramContentTable);
        contentScrollPane.setPreferredSize(new Dimension(480, 150));
        GridBagConstraints table = miniConstraints(1, 4, 1, 1, GridBagConstraints.BOTH);
        table.insets = new Insets(4, 0, 4, 0);
        miniProgramPanel.add(contentScrollPane, table);

        JPanel contentButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton addItemButton = new JButton("添加内容项");
        JButton removeItemButton = new JButton("删除选中项");
        contentButtons.add(addItemButton);
        contentButtons.add(removeItemButton);
        GridBagConstraints buttons = miniConstraints(1, 5, 1, 0, GridBagConstraints.HORIZONTAL);
        miniProgramPanel.add(contentButtons, buttons);

        JPanel optionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        miniProgramEmphasisFirstItemCheckBox = new JCheckBox("放大第一个内容项");
        enableIdTransCheckBox = new JCheckBox("开启 ID 转译");
        enableDuplicateCheckCheckBox = new JCheckBox("开启重复消息检查");
        optionPanel.add(miniProgramEmphasisFirstItemCheckBox);
        optionPanel.add(enableIdTransCheckBox);
        optionPanel.add(enableDuplicateCheckCheckBox);
        GridBagConstraints options = miniConstraints(1, 6, 1, 0, GridBagConstraints.HORIZONTAL);
        miniProgramPanel.add(optionPanel, options);

        JPanel intervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        intervalPanel.add(new JLabel("重复检查间隔（秒）"));
        duplicateCheckIntervalSpinner = new JSpinner(new SpinnerNumberModel(
                WxCpMiniProgramNoticeSupport.DEFAULT_DUPLICATE_CHECK_INTERVAL,
                1,
                WxCpMiniProgramNoticeSupport.MAX_DUPLICATE_CHECK_INTERVAL,
                60));
        intervalPanel.add(duplicateCheckIntervalSpinner);
        GridBagConstraints interval = miniConstraints(1, 7, 1, 0, GridBagConstraints.HORIZONTAL);
        miniProgramPanel.add(intervalPanel, interval);

        JLabel tips = new JLabel("<html>接收者使用人群数据第 1 列的企业微信 UserId；最多 10 个内容项，支持 $var0、$DATE 等变量，不支持 @all。</html>");
        GridBagConstraints tipConstraints = miniConstraints(0, 8, 1, 0, GridBagConstraints.HORIZONTAL);
        tipConstraints.gridwidth = 2;
        tipConstraints.insets = new Insets(8, 0, 0, 0);
        miniProgramPanel.add(tips, tipConstraints);

        addItemButton.addActionListener(e -> {
            if (miniProgramContentTableModel.getRowCount() >= WxCpMiniProgramNoticeSupport.MAX_CONTENT_ITEMS) {
                JOptionPane.showMessageDialog(wxCpMsgPanel, "内容项最多允许 10 个", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            miniProgramContentTableModel.addRow(new Object[]{"", ""});
        });
        removeItemButton.addActionListener(e -> {
            int selectedRow = miniProgramContentTable.getSelectedRow();
            if (selectedRow >= 0) {
                miniProgramContentTableModel.removeRow(selectedRow);
            }
        });
        enableDuplicateCheckCheckBox.addActionListener(e ->
                duplicateCheckIntervalSpinner.setEnabled(enableDuplicateCheckCheckBox.isSelected()));
        clearMiniProgramFields();
        miniProgramPanel.setVisible(false);
    }

    private void loadMiniProgramFields(TMsgWxCp message) {
        miniProgramAppIdTextField.setText(StringUtils.defaultString(message.getMiniProgramAppId()));
        miniProgramPageTextField.setText(StringUtils.defaultString(message.getMiniProgramPage()));
        miniProgramTitleTextField.setText(StringUtils.defaultString(message.getTitle()));
        miniProgramDescriptionTextField.setText(StringUtils.defaultString(message.getDescribe()));
        miniProgramEmphasisFirstItemCheckBox.setSelected(Boolean.TRUE.equals(message.getMiniProgramEmphasisFirstItem()));
        enableIdTransCheckBox.setSelected(Boolean.TRUE.equals(message.getEnableIdTrans()));
        enableDuplicateCheckCheckBox.setSelected(Boolean.TRUE.equals(message.getEnableDuplicateCheck()));
        duplicateCheckIntervalSpinner.setValue(message.getDuplicateCheckInterval() == null
                ? WxCpMiniProgramNoticeSupport.DEFAULT_DUPLICATE_CHECK_INTERVAL : message.getDuplicateCheckInterval());
        duplicateCheckIntervalSpinner.setEnabled(enableDuplicateCheckCheckBox.isSelected());
        miniProgramContentTableModel.setRowCount(0);
        List<WxCpMiniProgramContentItem> items = message.getMiniProgramContentItems();
        if (items != null) {
            for (WxCpMiniProgramContentItem item : items) {
                if (item != null) {
                    miniProgramContentTableModel.addRow(new Object[]{
                            StringUtils.defaultString(item.getKey()), StringUtils.defaultString(item.getValue())});
                }
            }
        }
        if (miniProgramContentTableModel.getRowCount() == 0) {
            miniProgramContentTableModel.addRow(new Object[]{"", ""});
        }
    }

    private void clearMiniProgramFields() {
        miniProgramAppIdTextField.setText("");
        miniProgramPageTextField.setText("");
        miniProgramTitleTextField.setText("");
        miniProgramDescriptionTextField.setText("");
        miniProgramEmphasisFirstItemCheckBox.setSelected(false);
        enableIdTransCheckBox.setSelected(false);
        enableDuplicateCheckCheckBox.setSelected(false);
        duplicateCheckIntervalSpinner.setValue(WxCpMiniProgramNoticeSupport.DEFAULT_DUPLICATE_CHECK_INTERVAL);
        duplicateCheckIntervalSpinner.setEnabled(false);
        miniProgramContentTableModel.setRowCount(0);
        miniProgramContentTableModel.addRow(new Object[]{"", ""});
    }

    private List<WxCpMiniProgramContentItem> collectMiniProgramContentItems() {
        if (miniProgramContentTable.isEditing()) {
            miniProgramContentTable.getCellEditor().stopCellEditing();
        }
        List<WxCpMiniProgramContentItem> items = new ArrayList<>();
        for (int row = 0; row < miniProgramContentTableModel.getRowCount(); row++) {
            String key = StringUtils.defaultString((String) miniProgramContentTableModel.getValueAt(row, 0));
            String value = StringUtils.defaultString((String) miniProgramContentTableModel.getValueAt(row, 1));
            if (StringUtils.isNotBlank(key) || StringUtils.isNotBlank(value)) {
                items.add(new WxCpMiniProgramContentItem(key, value));
            }
        }
        return items;
    }

    private void validateMiniProgramTemplate(List<WxCpMiniProgramContentItem> items) {
        String appId = miniProgramAppIdTextField.getText().trim();
        String page = miniProgramPageTextField.getText().trim();
        String title = miniProgramTitleTextField.getText().trim();
        String description = miniProgramDescriptionTextField.getText().trim();
        if (StringUtils.isBlank(appId)) {
            throw new IllegalArgumentException("小程序 AppId 不能为空");
        }
        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException("小程序通知标题不能为空");
        }
        boolean hasTemplate = containsTemplate(appId) || containsTemplate(page)
                || containsTemplate(title) || containsTemplate(description)
                || items.stream().anyMatch(item -> containsTemplate(item.getKey()) || containsTemplate(item.getValue()));
        if (!hasTemplate) {
            WxCpMiniProgramNoticeSupport.validateAndBuildContentItems(
                    appId, page, title, description, items,
                    enableDuplicateCheckCheckBox.isSelected(), (Integer) duplicateCheckIntervalSpinner.getValue());
        } else {
            if (page.getBytes(StandardCharsets.UTF_8).length > 1024) {
                throw new IllegalArgumentException("小程序页面路径不能超过 1024 字节");
            }
            if (items.size() > WxCpMiniProgramNoticeSupport.MAX_CONTENT_ITEMS) {
                throw new IllegalArgumentException("小程序通知内容项最多允许 10 个");
            }
            if (enableDuplicateCheckCheckBox.isSelected()) {
                int interval = (Integer) duplicateCheckIntervalSpinner.getValue();
                if (interval <= 0 || interval > WxCpMiniProgramNoticeSupport.MAX_DUPLICATE_CHECK_INTERVAL) {
                    throw new IllegalArgumentException("重复消息检查间隔必须在 1 到 14400 秒之间");
                }
            }
        }
    }

    private static boolean containsTemplate(String value) {
        return StringUtils.defaultString(value).contains("$");
    }

    private static GridBagConstraints miniConstraints(int x, int y, double weightX, double weightY, int fill) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.weightx = weightX;
        constraints.weighty = weightY;
        constraints.fill = fill;
        return constraints;
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
        wxCpMsgPanel.setLayout(new GridLayoutManager(9, 2, new Insets(10, 8, 0, 8), -1, -1));
        panel1.add(wxCpMsgPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        wxCpMsgPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, wxCpMsgPanel.getFont()), null));
        msgTypeLabel = new JLabel();
        msgTypeLabel.setText("消息类型");
        wxCpMsgPanel.add(msgTypeLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        wxCpMsgPanel.add(spacer1, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        msgTypeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("图文消息");
        defaultComboBoxModel1.addElement("文本消息");
        defaultComboBoxModel1.addElement("文本卡片消息");
        defaultComboBoxModel1.addElement("markdown消息");
        defaultComboBoxModel1.addElement("小程序通知消息");
        msgTypeComboBox.setModel(defaultComboBoxModel1);
        wxCpMsgPanel.add(msgTypeComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        titleLabel = new JLabel();
        titleLabel.setText("标题");
        wxCpMsgPanel.add(titleLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        picUrlLabel = new JLabel();
        picUrlLabel.setText("图片URL");
        wxCpMsgPanel.add(picUrlLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        picUrlTextField = new JTextField();
        wxCpMsgPanel.add(picUrlTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        descLabel = new JLabel();
        descLabel.setText("描述");
        wxCpMsgPanel.add(descLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        descTextField = new JTextArea();
        descTextField.setLineWrap(true);
        descTextField.setWrapStyleWord(true);
        wxCpMsgPanel.add(descTextField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        urlLabel = new JLabel();
        urlLabel.setText("跳转URL");
        wxCpMsgPanel.add(urlLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        urlTextField = new JTextField();
        wxCpMsgPanel.add(urlTextField, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        contentLabel = new JLabel();
        contentLabel.setText("内容");
        wxCpMsgPanel.add(contentLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        titleTextField = new JTextField();
        wxCpMsgPanel.add(titleTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(380, -1), new Dimension(380, -1), null, 0, false));
        contentTextArea = new JTextArea();
        contentTextArea.setLineWrap(true);
        contentTextArea.setWrapStyleWord(true);
        wxCpMsgPanel.add(contentTextArea, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        btnTxtLabel = new JLabel();
        btnTxtLabel.setText("按钮文字");
        btnTxtLabel.setToolTipText("可不填。默认为“详情”， 不超过4个文字，超过自动截断");
        wxCpMsgPanel.add(btnTxtLabel, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnTxtTextField = new JTextField();
        btnTxtTextField.setToolTipText("可不填。默认为“详情”， 不超过4个文字，超过自动截断");
        wxCpMsgPanel.add(btnTxtTextField, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        miniProgramPanel = new JPanel();
        miniProgramPanel.setLayout(new BorderLayout(0, 0));
        wxCpMsgPanel.add(miniProgramPanel, new GridConstraints(7, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
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
