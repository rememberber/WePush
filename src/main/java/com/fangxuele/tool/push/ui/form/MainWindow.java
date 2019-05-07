package com.fangxuele.tool.push.ui.form;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

/**
 * <pre>
 * 主界面
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/7.
 */
@Getter
public class MainWindow {
    private JPanel mainPanel;
    private JTabbedPane tabbedPane;
    private JPanel aboutPanel;
    private JSplitPane messagePanel;
    private JPanel memberPanel;
    private JPanel pushPanel;
    private JPanel helpPanel;
    private JPanel settingPanel;
    private JPanel schedulePanel;
    private JPanel pushHisPanel;
    private JPanel userCasePanel;
    private JPanel messageEditPanel;
    private JPanel messageManagePanel;
    private JPanel messageTypePanel;

    public static MainWindow mainWindow = new MainWindow();

    public void init() {
        mainWindow.getAboutPanel().add(AboutForm.aboutForm.getAboutPanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        mainWindow.getHelpPanel().add(HelpForm.helpForm.getHelpPanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        mainWindow.getUserCasePanel().add(UserCaseForm.userCaseForm.getUserCasePanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        mainWindow.getSchedulePanel().add(ScheduleForm.scheduleForm.getSchedulePanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        mainWindow.getPushHisPanel().add(PushHisForm.pushHisForm.getPushHisPanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        mainWindow.getSettingPanel().add(SettingForm.settingForm.getSettingPanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        mainWindow.getMessageEditPanel().add(MessageEditForm.messageEditForm.getMessageEditPanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        mainWindow.getMessageManagePanel().add(MessageManageForm.messageManageForm.getMessageManagePanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        mainWindow.getMemberPanel().add(MemberForm.memberForm.getMemberPanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        mainWindow.getPushPanel().add(PushForm.pushForm.getPushPanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        mainWindow.getMessageTypePanel().add(MessageTypeForm.messageTypeForm.getMessageTypePanel(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(8, 0, 0, 0), -1, -1));
        tabbedPane = new JTabbedPane();
        tabbedPane.setDoubleBuffered(true);
        Font tabbedPaneFont = this.$$$getFont$$$(null, -1, -1, tabbedPane.getFont());
        if (tabbedPaneFont != null) tabbedPane.setFont(tabbedPaneFont);
        mainPanel.add(tabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        aboutPanel = new JPanel();
        aboutPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane.addTab("关于", aboutPanel);
        messageTypePanel = new JPanel();
        messageTypePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("消息类型", messageTypePanel);
        messagePanel = new JSplitPane();
        messagePanel.setContinuousLayout(true);
        messagePanel.setDividerLocation(280);
        messagePanel.setDividerSize(4);
        messagePanel.setDoubleBuffered(true);
        tabbedPane.addTab("编辑消息", messagePanel);
        messageEditPanel = new JPanel();
        messageEditPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 8, 0, 8), -1, -1));
        messageEditPanel.setMaximumSize(new Dimension(-1, -1));
        messageEditPanel.setMinimumSize(new Dimension(-1, -1));
        messageEditPanel.setPreferredSize(new Dimension(-1, -1));
        messagePanel.setRightComponent(messageEditPanel);
        messageManagePanel = new JPanel();
        messageManagePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        messageManagePanel.setMaximumSize(new Dimension(-1, -1));
        messageManagePanel.setMinimumSize(new Dimension(-1, -1));
        messageManagePanel.setPreferredSize(new Dimension(280, -1));
        messagePanel.setLeftComponent(messageManagePanel);
        memberPanel = new JPanel();
        memberPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane.addTab("准备目标用户", memberPanel);
        pushPanel = new JPanel();
        pushPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane.addTab("开始推送", pushPanel);
        schedulePanel = new JPanel();
        schedulePanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane.addTab("计划任务", schedulePanel);
        pushHisPanel = new JPanel();
        pushHisPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane.addTab("推送历史管理", pushHisPanel);
        settingPanel = new JPanel();
        settingPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        Font settingPanelFont = this.$$$getFont$$$("Microsoft YaHei UI", -1, -1, settingPanel.getFont());
        if (settingPanelFont != null) settingPanel.setFont(settingPanelFont);
        tabbedPane.addTab("设置", settingPanel);
        helpPanel = new JPanel();
        helpPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 10, 0), -1, -1));
        tabbedPane.addTab("使用帮助", helpPanel);
        userCasePanel = new JPanel();
        userCasePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 10, 0), -1, -1));
        tabbedPane.addTab("他们都在用", userCasePanel);
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

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
