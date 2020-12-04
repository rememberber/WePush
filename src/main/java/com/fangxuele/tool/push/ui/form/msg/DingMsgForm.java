package com.fangxuele.tool.push.ui.form.msg;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.bean.DingMsg;
import com.fangxuele.tool.push.dao.TDingAppMapper;
import com.fangxuele.tool.push.dao.TMsgDingMapper;
import com.fangxuele.tool.push.domain.TDingApp;
import com.fangxuele.tool.push.domain.TMsgDing;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.dialog.CommonTipsDialog;
import com.fangxuele.tool.push.ui.dialog.DingAppDialog;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;
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
    private JLabel urlLabel;
    private JTextField urlTextField;
    private JLabel contentLabel;
    private JComboBox appNameComboBox;
    private JButton appManageButton;
    private JTextField titleTextField;
    private JTextArea contentTextArea;
    private JLabel btnTxtLabel;
    private JTextField btnTxtTextField;
    private JTextField btnURLTextField;
    private JLabel btnURLLabel;
    private JRadioButton workRadioButton;
    private JRadioButton robotRadioButton;
    private JTextField webHookTextField;
    private JLabel webHookHelpLabel;

    private static DingMsgForm dingMsgForm;

    private static TMsgDingMapper msgDingMapper = MybatisUtil.getSqlSession().getMapper(TMsgDingMapper.class);
    private static TDingAppMapper dingAppMapper = MybatisUtil.getSqlSession().getMapper(TDingAppMapper.class);

    public static Map<String, String> appNameToAgentIdMap = Maps.newHashMap();
    public static Map<String, String> agentIdToAppNameMap = Maps.newHashMap();

    public DingMsgForm() {
        // 消息类型切换事件
        msgTypeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                switchDingMsgType(e.getItem().toString());
            }
        });
        appManageButton.addActionListener(e -> {
            DingAppDialog dialog = new DingAppDialog();
            dialog.renderTable();
            dialog.pack();
            dialog.setVisible(true);
            initAppNameList();
        });

        workRadioButton.addChangeListener(e -> {
            boolean isSelected = workRadioButton.isSelected();
            if (isSelected) {
                robotRadioButton.setSelected(false);
            }
        });
        robotRadioButton.addChangeListener(e -> {
            boolean isSelected = robotRadioButton.isSelected();
            if (isSelected) {
                workRadioButton.setSelected(false);
            }
        });
        webHookHelpLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                CommonTipsDialog dialog = new CommonTipsDialog();

                StringBuilder tipsBuilder = new StringBuilder();
                tipsBuilder.append("<h1>如何获取自定义机器人webhook？</h1>");
                tipsBuilder.append("<p>进入一个钉钉群，在群的顶部功能栏中，点击【群设置】，进入菜单可以看到【群机器人】的入口，点击进入“群机器人”的管理面板后，可以进行添加、编辑和删除群机器人的操作。</p>");
                tipsBuilder.append("<p>在机器人管理页面选择“自定义”机器人，输入机器人名字并选择要发送消息的群。如果需要的话，可以为机器人设置一个头像。点击“完成添加”，完成后会生成Hook地址。</p>");
                tipsBuilder.append("<p>点击“复制”按钮，即可获得这个机器人对应的Webhook地址，其格式如下：</p>");
                tipsBuilder.append("<p>https://oapi.dingtalk.com/robot/send?access_token=xxxxxxxx</p>");

                dialog.setHtmlText(tipsBuilder.toString());
                dialog.pack();
                dialog.setVisible(true);

                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                label.setIcon(new ImageIcon(UiConsts.HELP_FOCUSED_ICON));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setIcon(new ImageIcon(UiConsts.HELP_ICON));
                super.mouseExited(e);
            }
        });
    }

    @Override
    public void init(String msgName) {
        clearAllField();
        initAppNameList();
        List<TMsgDing> tMsgDingList = msgDingMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.DING_CODE, msgName);
        if (tMsgDingList.size() > 0) {
            TMsgDing tMsgDing = tMsgDingList.get(0);
            String dingMsgType = tMsgDing.getDingMsgType();
            getInstance().getAppNameComboBox().setSelectedItem(agentIdToAppNameMap.get(tMsgDing.getAgentId()));
            getInstance().getMsgTypeComboBox().setSelectedItem(dingMsgType);
            DingMsg dingMsg = JSONUtil.toBean(tMsgDing.getContent(), DingMsg.class);
            getInstance().getContentTextArea().setText(dingMsg.getContent());
            getInstance().getTitleTextField().setText(dingMsg.getTitle());
            getInstance().getPicUrlTextField().setText(dingMsg.getPicUrl());
            getInstance().getUrlTextField().setText(dingMsg.getUrl());
            getInstance().getBtnTxtTextField().setText(dingMsg.getBtnTxt());
            getInstance().getWebHookTextField().setText(tMsgDing.getWebHook());

            switchDingMsgType(dingMsgType);

            switchRadio(tMsgDing.getRadioType());

            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            messageEditForm.getMsgNameField().setText(tMsgDing.getMsgName());
            messageEditForm.getPreviewUserField().setText(tMsgDing.getPreviewUser());
        } else {
            switchDingMsgType("文本消息");
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

        List<TMsgDing> tMsgDingList = msgDingMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.DING_CODE, msgName);
        if (tMsgDingList.size() > 0) {
            existSameMsg = true;
        }

        int isCover = JOptionPane.NO_OPTION;
        if (existSameMsg) {
            // 如果存在，是否覆盖
            isCover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(), "已经存在同名的历史消息，\n是否覆盖？", "确认",
                    JOptionPane.YES_NO_OPTION);
        }

        if (!existSameMsg || isCover == JOptionPane.YES_OPTION) {
            String dingMsgType = Objects.requireNonNull(getInstance().getMsgTypeComboBox().getSelectedItem()).toString();
            String content = getInstance().getContentTextArea().getText();
            String title = getInstance().getTitleTextField().getText();
            String picUrl = getInstance().getPicUrlTextField().getText();
            String url = getInstance().getUrlTextField().getText();
            String btnTxt = getInstance().getBtnTxtTextField().getText();
            String btnUrl = getInstance().getBtnURLTextField().getText();
            String webHook = getInstance().getWebHookTextField().getText();

            String now = SqliteUtil.nowDateForSqlite();

            TMsgDing tMsgDing = new TMsgDing();
            tMsgDing.setMsgType(MessageTypeEnum.DING_CODE);
            tMsgDing.setMsgName(msgName);
            tMsgDing.setAgentId(appNameToAgentIdMap.get(getInstance().getAppNameComboBox().getSelectedItem()));
            tMsgDing.setDingMsgType(dingMsgType);
            DingMsg dingMsg = new DingMsg();
            dingMsg.setContent(content);
            dingMsg.setTitle(title);
            dingMsg.setPicUrl(picUrl);
            dingMsg.setUrl(url);
            dingMsg.setBtnTxt(btnTxt);
            dingMsg.setBtnUrl(btnUrl);

            tMsgDing.setContent(JSONUtil.toJsonStr(dingMsg));
            tMsgDing.setModifiedTime(now);
            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            tMsgDing.setPreviewUser(messageEditForm.getPreviewUserField().getText());

            if (getInstance().getWorkRadioButton().isSelected()) {
                tMsgDing.setRadioType("work");
            } else {
                tMsgDing.setRadioType("robot");
            }
            tMsgDing.setWebHook(webHook);

            if (existSameMsg) {
                msgDingMapper.updateByMsgTypeAndMsgName(tMsgDing);
            } else {
                tMsgDing.setCreateTime(now);
                msgDingMapper.insertSelective(tMsgDing);
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
    public static void switchDingMsgType(String msgType) {
        switch (msgType) {
            case "文本消息":
                getInstance().getContentTextArea().setVisible(true);
                getInstance().getPicUrlLabel().setVisible(false);
                getInstance().getPicUrlTextField().setVisible(false);
                getInstance().getUrlLabel().setVisible(false);
                getInstance().getUrlTextField().setVisible(false);
                getInstance().getTitleLabel().setVisible(false);
                getInstance().getTitleTextField().setVisible(false);
                getInstance().getBtnTxtLabel().setVisible(false);
                getInstance().getBtnTxtTextField().setVisible(false);
                getInstance().getBtnURLLabel().setVisible(false);
                getInstance().getBtnURLTextField().setVisible(false);
                break;
            case "markdown消息":
                getInstance().getContentLabel().setVisible(true);
                getInstance().getContentTextArea().setVisible(true);
                getInstance().getPicUrlLabel().setVisible(false);
                getInstance().getPicUrlTextField().setVisible(false);
                getInstance().getUrlLabel().setVisible(false);
                getInstance().getUrlTextField().setVisible(false);
                getInstance().getTitleLabel().setVisible(true);
                getInstance().getTitleTextField().setVisible(true);
                getInstance().getBtnTxtLabel().setVisible(false);
                getInstance().getBtnTxtTextField().setVisible(false);
                getInstance().getBtnURLLabel().setVisible(false);
                getInstance().getBtnURLTextField().setVisible(false);
                break;
            case "链接消息":
                getInstance().getContentLabel().setVisible(true);
                getInstance().getContentTextArea().setVisible(true);
                getInstance().getBtnTxtLabel().setVisible(false);
                getInstance().getBtnTxtTextField().setVisible(false);
                getInstance().getPicUrlLabel().setVisible(true);
                getInstance().getPicUrlTextField().setVisible(true);
                getInstance().getUrlLabel().setVisible(true);
                getInstance().getUrlTextField().setVisible(true);
                getInstance().getTitleLabel().setVisible(true);
                getInstance().getTitleTextField().setVisible(true);
                getInstance().getBtnURLLabel().setVisible(false);
                getInstance().getBtnURLTextField().setVisible(false);
                break;
            case "卡片消息":
                getInstance().getTitleLabel().setVisible(true);
                getInstance().getTitleTextField().setVisible(true);
                getInstance().getContentLabel().setVisible(true);
                getInstance().getContentTextArea().setVisible(true);
                getInstance().getBtnTxtLabel().setVisible(true);
                getInstance().getBtnTxtTextField().setVisible(true);
                getInstance().getPicUrlLabel().setVisible(false);
                getInstance().getPicUrlTextField().setVisible(false);
                getInstance().getUrlLabel().setVisible(false);
                getInstance().getUrlTextField().setVisible(false);
                getInstance().getBtnURLLabel().setVisible(true);
                getInstance().getBtnURLTextField().setVisible(true);
                break;
            default:
                break;
        }
    }

    private void switchRadio(String radioType) {
        getInstance().getWorkRadioButton().setSelected(false);
        getInstance().getRobotRadioButton().setSelected(false);
        if ("work".equals(radioType)) {
            getInstance().getWorkRadioButton().setSelected(true);
        } else if ("robot".equals(radioType)) {
            getInstance().getRobotRadioButton().setSelected(true);
        }
    }

    /**
     * 清空所有界面字段
     */
    public static void clearAllField() {
        getInstance().getContentTextArea().setText("");
        getInstance().getTitleTextField().setText("");
        getInstance().getPicUrlTextField().setText("");
        getInstance().getUrlTextField().setText("");
        getInstance().getBtnTxtTextField().setText("");
        getInstance().getBtnURLTextField().setText("");
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
        dingMsgPanel.setLayout(new GridLayoutManager(9, 2, new Insets(10, 8, 0, 8), -1, -1));
        panel1.add(dingMsgPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        dingMsgPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, dingMsgPanel.getFont()), null));
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
        dingMsgPanel.add(msgTypeComboBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        titleLabel = new JLabel();
        titleLabel.setText("标题");
        dingMsgPanel.add(titleLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        picUrlLabel = new JLabel();
        picUrlLabel.setText("图片URL");
        dingMsgPanel.add(picUrlLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        picUrlTextField = new JTextField();
        dingMsgPanel.add(picUrlTextField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        urlLabel = new JLabel();
        urlLabel.setText("跳转URL");
        dingMsgPanel.add(urlLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        urlTextField = new JTextField();
        dingMsgPanel.add(urlTextField, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        contentLabel = new JLabel();
        contentLabel.setText("内容");
        dingMsgPanel.add(contentLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        titleTextField = new JTextField();
        dingMsgPanel.add(titleTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(380, -1), new Dimension(380, -1), null, 0, false));
        contentTextArea = new JTextArea();
        dingMsgPanel.add(contentTextArea, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        btnTxtLabel = new JLabel();
        btnTxtLabel.setText("按钮文字");
        btnTxtLabel.setToolTipText("可不填。默认为“详情”， 不超过4个文字，超过自动截断");
        dingMsgPanel.add(btnTxtLabel, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnTxtTextField = new JTextField();
        btnTxtTextField.setToolTipText("可不填。默认为“详情”， 不超过4个文字，超过自动截断");
        dingMsgPanel.add(btnTxtTextField, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        btnURLTextField = new JTextField();
        dingMsgPanel.add(btnURLTextField, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        btnURLLabel = new JLabel();
        btnURLLabel.setText("按钮URL");
        dingMsgPanel.add(btnURLLabel, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 20, 0), -1, -1));
        dingMsgPanel.add(panel2, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        appNameComboBox = new JComboBox();
        panel2.add(appNameComboBox, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        appManageButton = new JButton();
        appManageButton.setText("应用管理");
        panel2.add(appManageButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        workRadioButton = new JRadioButton();
        workRadioButton.setText("工作通知消息");
        panel2.add(workRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        robotRadioButton = new JRadioButton();
        robotRadioButton.setText("群机器人消息");
        panel2.add(robotRadioButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        webHookTextField = new JTextField();
        panel2.add(webHookTextField, new GridConstraints(1, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        webHookHelpLabel = new JLabel();
        webHookHelpLabel.setIcon(new ImageIcon(getClass().getResource("/icon/helpButton.png")));
        webHookHelpLabel.setText("webhook");
        panel2.add(webHookHelpLabel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel1.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel1.add(spacer3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        msgTypeLabel.setLabelFor(msgTypeComboBox);
        titleLabel.setLabelFor(titleTextField);
        picUrlLabel.setLabelFor(picUrlTextField);
        urlLabel.setLabelFor(urlTextField);
        btnURLLabel.setLabelFor(btnURLTextField);
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
