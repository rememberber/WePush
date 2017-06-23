package com.fangxuele.tool.wechat.push.ui;

import com.fangxuele.tool.wechat.push.ui.listener.AboutListener;
import com.fangxuele.tool.wechat.push.ui.listener.FramListener;
import com.fangxuele.tool.wechat.push.ui.listener.MemberListener;
import com.fangxuele.tool.wechat.push.ui.listener.MsgListener;
import com.fangxuele.tool.wechat.push.ui.listener.PushListener;
import com.fangxuele.tool.wechat.push.ui.listener.SettingListener;
import com.fangxuele.tool.wechat.push.ui.listener.TabListener;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.LogFactory;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Toolkit;

/**
 * Created by zhouy on 2017/6/7.
 */
public class MainWindow {
    private JPanel mainPanel;
    private JTabbedPane tabbedPane;
    private JPanel aboutPanel;
    private JPanel messagePanel;
    private JPanel memberPanel;
    private JPanel pushPanel;
    private JPanel reportPanel;
    private JPanel settingPanel;
    private JLabel companyLabel;
    private JLabel versionLabel;
    private JComboBox msgTypeComboBox;
    private JLabel msgTypeLabel;
    private JPanel templateMsgPanel;
    private JPanel kefuMsgPanel;
    private JTextField previewUserField;
    private JButton previewMsgButton;
    private JLabel previewMemberLabel;
    private JTextField msgTemplateIdTextField;
    private JTextField msgTemplateUrlTextField;
    private JPanel templateMsgDataPanel;
    private JLabel templateIdLabel;
    private JLabel templateUrlLabel;
    private JTextField templateDataNameTextField;
    private JTextField templateDataValueTextField;
    private JTextField templateDataColorTextField;
    private JButton templateMsgDataAddButton;
    private JTable templateMsgDataTable;
    private JLabel templateMsgNameLabel;
    private JLabel templateMsgValueLabel;
    private JLabel templateMsgColorLabel;
    private JComboBox msgKefuMsgTypeComboBox;
    private JTextField msgKefuMsgTitleTextField;
    private JTextField msgKefuPicUrlTextField;
    private JTextField msgKefuDescTextField;
    private JTextField msgKefuUrlTextField;
    private JLabel kefuMsgTypeLabel;
    private JLabel kefuMsgTitleLabel;
    private JLabel kefuMsgPicUrlLabel;
    private JLabel kefuMsgDescLabel;
    private JLabel kefuMsgUrlLabel;
    private JProgressBar memberTabImportProgressBar;
    private JTextArea importFromSqlTextArea;
    private JButton importFromSqlButton;
    private JTextField memberFilePathField;
    private JButton importFromFileButton;
    private JPanel memberTabUpPanel;
    private JPanel memberTabDownPanel;
    private JPanel memberTabCenterPanel;
    private JLabel memberTabCountLabel;
    private JLabel memberTabImportProgressLabel;
    private JLabel importFromFileLabel;
    private JProgressBar pushTotalProgressBar;
    private JTextField pushPageSizeTextField;
    private JTextField pushPagePerThreadTextField;
    private JTextArea pushConsoleTextArea;
    private JTable pushThreadTable;
    private JButton ScheduleRunButton;
    private JLabel pushSuccessCount;
    private JLabel pushFailCount;
    private JButton pushStopButton;
    private JButton pushStartButton;
    private JTextField msgNameField;
    private JComboBox msgHistoryComboBox;
    private JButton msgSaveButton;
    private JLabel msgHistoryLabel;
    private JLabel msgNameLabel;
    private JPanel pushUpPanel;
    private JPanel pushDownPanel;
    private JPanel pushCenterPanel;
    private JPanel pushControlPanel;
    private JLabel pushTotalCountLabel;
    private JLabel pushTotalPageLabel;
    private JLabel pushTotalThreadLabel;
    private JLabel pushTotalProgressLabel;
    private JRadioButton runAtThisTimeRadioButton;
    private JTextField startAtThisTimeTextField;
    private JPanel schedulePanel;
    private JRadioButton stopAtThisTimeRadioButton;
    private JTextField stopAtThisTimeTextField;
    private JRadioButton runPerDayRadioButton;
    private JTextField startPerDayTextField;
    private JRadioButton runPerWeekRadioButton;
    private JButton scheduleSaveButton;
    private JComboBox schedulePerWeekComboBox;
    private JTextField startPerWeekTextField;
    private JTextField wechatAppIdTextField;
    private JPasswordField wechatAppSecretPasswordField;
    private JPasswordField wechatTokenPasswordField;
    private JPasswordField wechatAesKeyPasswordField;
    private JButton settingMpInfoSaveButton;
    private JTextField aliServerUrlTextField;
    private JPasswordField aliAppKeyPasswordField;
    private JTextField aliSignTextField;
    private JButton settingAliInfoSaveButton;
    private JTextField mysqlUrlTextField;
    private JTextField mysqlDatabaseTextField;
    private JTextField mysqlUserTextField;
    private JPasswordField mysqlPasswordField;
    private JButton settingTestDbLinkButton;
    private JButton settingDbInfoSaveButton;
    private JTable msgHistable;
    private JCheckBox dryRunCheckBox;
    private JLabel sloganLabel;
    private JLabel checkUpdateLabel;
    private JButton createMsgButton;
    private JButton memberImportAllButton;
    private JComboBox settingThemeComboBox;
    private JComboBox settingFontNameComboBox;
    private JComboBox settingFontSizeComboBox;
    private JButton settingAppearanceSaveButton;
    private JLabel pushLastTimeLabel;
    private JLabel pushLeftTimeLabel;
    private JLabel pushMsgName;
    private JButton pushPauseButton;
    private JScrollPane settingScrollPane;
    private JPasswordField aliAppSecretPasswordField;
    private JButton clearImportButton;
    private JComboBox memberHisComboBox;
    private JButton importFromHisButton;
    private JTable importHisTable;
    private JPanel memberHisManagePanel;
    private JButton msgHisTableSelectAllButton;
    private JButton msgHisTableDeleteButton;
    private JButton importHisTableSelectAllButton;
    private JButton importHisTableDeleteButton;
    private JButton msgHisTableUnselectAllButton;
    private JButton importHisTableUnselectAllButton;
    public static JFrame frame;

    public static MainWindow mainWindow;
    private static Log logger = LogFactory.get();

    public MainWindow() {

    }

    public static void main(String[] args) {
        logger.info("main start");

        Init.initTheme();
        Init.initGlobalFont();  //统一设置字体

        frame = new JFrame(ConstantsUI.APP_NAME);
        frame.setIconImage(ConstantsUI.IMAGE_ICON);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); //得到屏幕的尺寸
        frame.setBounds((int) (screenSize.width * 0.1), (int) (screenSize.height * 0.08), (int) (screenSize.width * 0.8),
                (int) (screenSize.height * 0.8));

        Dimension preferSize = new Dimension((int) (screenSize.width * 0.8),
                (int) (screenSize.height * 0.8));
        frame.setPreferredSize(preferSize);

        mainWindow = new MainWindow();

        frame.setContentPane(mainWindow.mainPanel);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        Init.initOthers();
        Init.initAllTab();

        AboutListener.addListeners();
        SettingListener.addListeners();
        MsgListener.addListeners();
        MemberListener.addListeners();
        PushListener.addListeners();
        TabListener.addListeners();
        FramListener.addListeners();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void setMainPanel(JPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public void setTabbedPane(JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
    }

    public JPanel getAboutPanel() {
        return aboutPanel;
    }

    public void setAboutPanel(JPanel aboutPanel) {
        this.aboutPanel = aboutPanel;
    }

    public JPanel getMessagePanel() {
        return messagePanel;
    }

    public void setMessagePanel(JPanel messagePanel) {
        this.messagePanel = messagePanel;
    }

    public JPanel getMemberPanel() {
        return memberPanel;
    }

    public void setMemberPanel(JPanel memberPanel) {
        this.memberPanel = memberPanel;
    }

    public JPanel getPushPanel() {
        return pushPanel;
    }

    public void setPushPanel(JPanel pushPanel) {
        this.pushPanel = pushPanel;
    }

    public JPanel getReportPanel() {
        return reportPanel;
    }

    public void setReportPanel(JPanel reportPanel) {
        this.reportPanel = reportPanel;
    }

    public JPanel getSettingPanel() {
        return settingPanel;
    }

    public void setSettingPanel(JPanel settingPanel) {
        this.settingPanel = settingPanel;
    }

    public JLabel getCompanyLabel() {
        return companyLabel;
    }

    public void setCompanyLabel(JLabel companyLabel) {
        this.companyLabel = companyLabel;
    }

    public JLabel getVersionLabel() {
        return versionLabel;
    }

    public void setVersionLabel(JLabel versionLabel) {
        this.versionLabel = versionLabel;
    }

    public JComboBox getMsgTypeComboBox() {
        return msgTypeComboBox;
    }

    public void setMsgTypeComboBox(String msgType) {
        this.msgTypeComboBox.setSelectedItem(msgType);
    }

    public JLabel getMsgTypeLabel() {
        return msgTypeLabel;
    }

    public void setMsgTypeLabel(JLabel msgTypeLabel) {
        this.msgTypeLabel = msgTypeLabel;
    }

    public JPanel getTemplateMsgPanel() {
        return templateMsgPanel;
    }

    public void setTemplateMsgPanel(JPanel templateMsgPanel) {
        this.templateMsgPanel = templateMsgPanel;
    }

    public JPanel getKefuMsgPanel() {
        return kefuMsgPanel;
    }

    public void setKefuMsgPanel(JPanel kefuMsgPanel) {
        this.kefuMsgPanel = kefuMsgPanel;
    }

    public JTextField getPreviewUserField() {
        return previewUserField;
    }

    public void setPreviewUserField(String previewUser) {
        this.previewUserField.setText(previewUser);
    }

    public JButton getPreviewMsgButton() {
        return previewMsgButton;
    }

    public void setPreviewMsgButton(JButton previewMsgButton) {
        this.previewMsgButton = previewMsgButton;
    }

    public JLabel getPreviewMemberLabel() {
        return previewMemberLabel;
    }

    public void setPreviewMemberLabel(JLabel previewMemberLabel) {
        this.previewMemberLabel = previewMemberLabel;
    }

    public JTextField getMsgTemplateIdTextField() {
        return msgTemplateIdTextField;
    }

    public void setMsgTemplateIdTextField(String msgTemplateIdTextField) {
        this.msgTemplateIdTextField.setText(msgTemplateIdTextField);
    }

    public JTextField getMsgTemplateUrlTextField() {
        return msgTemplateUrlTextField;
    }

    public void setMsgTemplateUrlTextField(String msgTemplateUrlTextField) {
        this.msgTemplateUrlTextField.setText(msgTemplateUrlTextField);
    }

    public JPanel getTemplateMsgDataPanel() {
        return templateMsgDataPanel;
    }

    public void setTemplateMsgDataPanel(JPanel templateMsgDataPanel) {
        this.templateMsgDataPanel = templateMsgDataPanel;
    }

    public JLabel getTemplateIdLabel() {
        return templateIdLabel;
    }

    public void setTemplateIdLabel(JLabel templateIdLabel) {
        this.templateIdLabel = templateIdLabel;
    }

    public JLabel getTemplateUrlLabel() {
        return templateUrlLabel;
    }

    public void setTemplateUrlLabel(JLabel templateUrlLabel) {
        this.templateUrlLabel = templateUrlLabel;
    }

    public JTextField getTemplateDataNameTextField() {
        return templateDataNameTextField;
    }

    public void setTemplateDataNameTextField(JTextField templateDataNameTextField) {
        this.templateDataNameTextField = templateDataNameTextField;
    }

    public JTextField getTemplateDataValueTextField() {
        return templateDataValueTextField;
    }

    public void setTemplateDataValueTextField(JTextField templateDataValueTextField) {
        this.templateDataValueTextField = templateDataValueTextField;
    }

    public JTextField getTemplateDataColorTextField() {
        return templateDataColorTextField;
    }

    public void setTemplateDataColorTextField(JTextField templateDataColorTextField) {
        this.templateDataColorTextField = templateDataColorTextField;
    }

    public JButton getTemplateMsgDataAddButton() {
        return templateMsgDataAddButton;
    }

    public void setTemplateMsgDataAddButton(JButton templateMsgDataAddButton) {
        this.templateMsgDataAddButton = templateMsgDataAddButton;
    }

    public JTable getTemplateMsgDataTable() {
        return templateMsgDataTable;
    }

    public void setTemplateMsgDataTable(JTable templateMsgDataTable) {
        this.templateMsgDataTable = templateMsgDataTable;
    }

    public JLabel getTemplateMsgNameLabel() {
        return templateMsgNameLabel;
    }

    public void setTemplateMsgNameLabel(JLabel templateMsgNameLabel) {
        this.templateMsgNameLabel = templateMsgNameLabel;
    }

    public JLabel getTemplateMsgValueLabel() {
        return templateMsgValueLabel;
    }

    public void setTemplateMsgValueLabel(JLabel templateMsgValueLabel) {
        this.templateMsgValueLabel = templateMsgValueLabel;
    }

    public JLabel getTemplateMsgColorLabel() {
        return templateMsgColorLabel;
    }

    public void setTemplateMsgColorLabel(JLabel templateMsgColorLabel) {
        this.templateMsgColorLabel = templateMsgColorLabel;
    }

    public JComboBox getMsgKefuMsgTypeComboBox() {
        return msgKefuMsgTypeComboBox;
    }

    public void setMsgKefuMsgTypeComboBox(String msgKefuMsgTypeComboBox) {
        this.msgKefuMsgTypeComboBox.setSelectedItem(msgKefuMsgTypeComboBox);
    }

    public JTextField getMsgKefuMsgTitleTextField() {
        return msgKefuMsgTitleTextField;
    }

    public void setMsgKefuMsgTitleTextField(String msgKefuMsgTitleTextField) {
        this.msgKefuMsgTitleTextField.setText(msgKefuMsgTitleTextField);
    }

    public JTextField getMsgKefuPicUrlTextField() {
        return msgKefuPicUrlTextField;
    }

    public void setMsgKefuPicUrlTextField(String msgKefuPicUrlTextField) {
        this.msgKefuPicUrlTextField.setText(msgKefuPicUrlTextField);
    }

    public JTextField getMsgKefuDescTextField() {
        return msgKefuDescTextField;
    }

    public void setMsgKefuDescTextField(String msgKefuDescTextField) {
        this.msgKefuDescTextField.setText(msgKefuDescTextField);
    }

    public JTextField getMsgKefuUrlTextField() {
        return msgKefuUrlTextField;
    }

    public void setMsgKefuUrlTextField(String msgKefuUrlTextField) {
        this.msgKefuUrlTextField.setText(msgKefuUrlTextField);
    }

    public JLabel getKefuMsgTypeLabel() {
        return kefuMsgTypeLabel;
    }

    public void setKefuMsgTypeLabel(JLabel kefuMsgTypeLabel) {
        this.kefuMsgTypeLabel = kefuMsgTypeLabel;
    }

    public JLabel getKefuMsgTitleLabel() {
        return kefuMsgTitleLabel;
    }

    public void setKefuMsgTitleLabel(JLabel kefuMsgTitleLabel) {
        this.kefuMsgTitleLabel = kefuMsgTitleLabel;
    }

    public JLabel getKefuMsgPicUrlLabel() {
        return kefuMsgPicUrlLabel;
    }

    public void setKefuMsgPicUrlLabel(JLabel kefuMsgPicUrlLabel) {
        this.kefuMsgPicUrlLabel = kefuMsgPicUrlLabel;
    }

    public JLabel getKefuMsgDescLabel() {
        return kefuMsgDescLabel;
    }

    public void setKefuMsgDescLabel(JLabel kefuMsgDescLabel) {
        this.kefuMsgDescLabel = kefuMsgDescLabel;
    }

    public JLabel getKefuMsgUrlLabel() {
        return kefuMsgUrlLabel;
    }

    public void setKefuMsgUrlLabel(JLabel kefuMsgUrlLabel) {
        this.kefuMsgUrlLabel = kefuMsgUrlLabel;
    }

    public JProgressBar getMemberTabImportProgressBar() {
        return memberTabImportProgressBar;
    }

    public void setMemberTabImportProgressBar(JProgressBar memberTabImportProgressBar) {
        this.memberTabImportProgressBar = memberTabImportProgressBar;
    }

    public JTextArea getImportFromSqlTextArea() {
        return importFromSqlTextArea;
    }

    public void setImportFromSqlTextArea(String importFromSql) {
        this.importFromSqlTextArea.setText(importFromSql);
    }

    public JButton getImportFromSqlButton() {
        return importFromSqlButton;
    }

    public void setImportFromSqlButton(JButton importFromSqlButton) {
        this.importFromSqlButton = importFromSqlButton;
    }

    public JTextField getMemberFilePathField() {
        return memberFilePathField;
    }

    public void setMemberFilePathField(String memberFilePath) {
        this.memberFilePathField.setText(memberFilePath);
    }

    public JButton getImportFromFileButton() {
        return importFromFileButton;
    }

    public void setImportFromFileButton(JButton importFromFileButton) {
        this.importFromFileButton = importFromFileButton;
    }

    public JPanel getMemberTabUpPanel() {
        return memberTabUpPanel;
    }

    public void setMemberTabUpPanel(JPanel memberTabUpPanel) {
        this.memberTabUpPanel = memberTabUpPanel;
    }

    public JPanel getMemberTabDownPanel() {
        return memberTabDownPanel;
    }

    public void setMemberTabDownPanel(JPanel memberTabDownPanel) {
        this.memberTabDownPanel = memberTabDownPanel;
    }

    public JPanel getMemberTabCenterPanel() {
        return memberTabCenterPanel;
    }

    public void setMemberTabCenterPanel(JPanel memberTabCenterPanel) {
        this.memberTabCenterPanel = memberTabCenterPanel;
    }

    public JLabel getMemberTabCountLabel() {
        return memberTabCountLabel;
    }

    public void setMemberTabCountLabel(int memberTabCount) {
        this.memberTabCountLabel.setText(String.valueOf(memberTabCount));
    }

    public JLabel getMemberTabImportProgressLabel() {
        return memberTabImportProgressLabel;
    }

    public void setMemberTabImportProgressLabel(JLabel memberTabImportProgressLabel) {
        this.memberTabImportProgressLabel = memberTabImportProgressLabel;
    }

    public JLabel getImportFromFileLabel() {
        return importFromFileLabel;
    }

    public void setImportFromFileLabel(JLabel importFromFileLabel) {
        this.importFromFileLabel = importFromFileLabel;
    }

    public JProgressBar getPushTotalProgressBar() {
        return pushTotalProgressBar;
    }

    public void setPushTotalProgressBar(JProgressBar pushTotalProgressBar) {
        this.pushTotalProgressBar = pushTotalProgressBar;
    }

    public JTextField getPushPageSizeTextField() {
        return pushPageSizeTextField;
    }

    public void setPushPageSizeTextField(int pushPageSize) {
        this.pushPageSizeTextField.setText(String.valueOf(pushPageSize));
    }

    public JTextField getPushPagePerThreadTextField() {
        return pushPagePerThreadTextField;
    }

    public void setPushPagePerThreadTextField(int pushPagePerThread) {
        this.pushPagePerThreadTextField.setText(String.valueOf(pushPagePerThread));
    }

    public JTextArea getPushConsoleTextArea() {
        return pushConsoleTextArea;
    }

    public void setPushConsoleTextArea(JTextArea pushConsoleTextArea) {
        this.pushConsoleTextArea = pushConsoleTextArea;
    }

    public JTable getPushThreadTable() {
        return pushThreadTable;
    }

    public void setPushThreadTable(JTable pushThreadTable) {
        this.pushThreadTable = pushThreadTable;
    }

    public JButton getScheduleRunButton() {
        return ScheduleRunButton;
    }

    public void setScheduleRunButton(JButton scheduleRunButton) {
        ScheduleRunButton = scheduleRunButton;
    }

    public JLabel getPushSuccessCount() {
        return pushSuccessCount;
    }

    public void setPushSuccessCount(JLabel pushSuccessCount) {
        this.pushSuccessCount = pushSuccessCount;
    }

    public JLabel getPushFailCount() {
        return pushFailCount;
    }

    public void setPushFailCount(JLabel pushFailCount) {
        this.pushFailCount = pushFailCount;
    }

    public JButton getPushStopButton() {
        return pushStopButton;
    }

    public void setPushStopButton(JButton pushStopButton) {
        this.pushStopButton = pushStopButton;
    }

    public JButton getPushStartButton() {
        return pushStartButton;
    }

    public void setPushStartButton(JButton pushStartButton) {
        this.pushStartButton = pushStartButton;
    }

    public JTextField getMsgNameField() {
        return msgNameField;
    }

    public void setMsgNameField(String msgName) {
        this.msgNameField.setText(msgName);
    }

    public JComboBox getMsgHistoryComboBox() {
        return msgHistoryComboBox;
    }

    public void setMsgHistoryComboBox(JComboBox msgHistoryComboBox) {
        this.msgHistoryComboBox = msgHistoryComboBox;
    }

    public JButton getMsgSaveButton() {
        return msgSaveButton;
    }

    public void setMsgSaveButton(JButton msgSaveButton) {
        this.msgSaveButton = msgSaveButton;
    }

    public JLabel getMsgHistoryLabel() {
        return msgHistoryLabel;
    }

    public void setMsgHistoryLabel(JLabel msgHistoryLabel) {
        this.msgHistoryLabel = msgHistoryLabel;
    }

    public JLabel getMsgNameLabel() {
        return msgNameLabel;
    }

    public void setMsgNameLabel(JLabel msgNameLabel) {
        this.msgNameLabel = msgNameLabel;
    }

    public JPanel getPushUpPanel() {
        return pushUpPanel;
    }

    public void setPushUpPanel(JPanel pushUpPanel) {
        this.pushUpPanel = pushUpPanel;
    }

    public JPanel getPushDownPanel() {
        return pushDownPanel;
    }

    public void setPushDownPanel(JPanel pushDownPanel) {
        this.pushDownPanel = pushDownPanel;
    }

    public JPanel getPushCenterPanel() {
        return pushCenterPanel;
    }

    public void setPushCenterPanel(JPanel pushCenterPanel) {
        this.pushCenterPanel = pushCenterPanel;
    }

    public JPanel getPushControlPanel() {
        return pushControlPanel;
    }

    public void setPushControlPanel(JPanel pushControlPanel) {
        this.pushControlPanel = pushControlPanel;
    }

    public JLabel getPushTotalCountLabel() {
        return pushTotalCountLabel;
    }

    public void setPushTotalCountLabel(JLabel pushTotalCountLabel) {
        this.pushTotalCountLabel = pushTotalCountLabel;
    }

    public JLabel getPushTotalPageLabel() {
        return pushTotalPageLabel;
    }

    public void setPushTotalPageLabel(JLabel pushTotalPageLabel) {
        this.pushTotalPageLabel = pushTotalPageLabel;
    }

    public JLabel getPushTotalThreadLabel() {
        return pushTotalThreadLabel;
    }

    public void setPushTotalThreadLabel(JLabel pushTotalThreadLabel) {
        this.pushTotalThreadLabel = pushTotalThreadLabel;
    }

    public JLabel getPushTotalProgressLabel() {
        return pushTotalProgressLabel;
    }

    public void setPushTotalProgressLabel(JLabel pushTotalProgressLabel) {
        this.pushTotalProgressLabel = pushTotalProgressLabel;
    }

    public JRadioButton getRunAtThisTimeRadioButton() {
        return runAtThisTimeRadioButton;
    }

    public void setRunAtThisTimeRadioButton(boolean runAtThisTime) {
        this.runAtThisTimeRadioButton.setSelected(runAtThisTime);
    }

    public JTextField getStartAtThisTimeTextField() {
        return startAtThisTimeTextField;
    }

    public void setStartAtThisTimeTextField(String startAtThisTimeTextField) {
        this.startAtThisTimeTextField.setText(startAtThisTimeTextField);
    }

    public JPanel getSchedulePanel() {
        return schedulePanel;
    }

    public void setSchedulePanel(JPanel schedulePanel) {
        this.schedulePanel = schedulePanel;
    }

    public JRadioButton getStopAtThisTimeRadioButton() {
        return stopAtThisTimeRadioButton;
    }

    public void setStopAtThisTimeRadioButton(boolean stopAtThisTime) {
        this.stopAtThisTimeRadioButton.setSelected(stopAtThisTime);
    }

    public JTextField getStopAtThisTimeTextField() {
        return stopAtThisTimeTextField;
    }

    public void setStopAtThisTimeTextField(String stopAtThisTimeTextField) {
        this.stopAtThisTimeTextField.setText(stopAtThisTimeTextField);
    }

    public JRadioButton getRunPerDayRadioButton() {
        return runPerDayRadioButton;
    }

    public void setRunPerDayRadioButton(boolean runPerDay) {
        this.runPerDayRadioButton.setSelected(runPerDay);
    }

    public JTextField getStartPerDayTextField() {
        return startPerDayTextField;
    }

    public void setStartPerDayTextField(String startPerDayTextField) {
        this.startPerDayTextField.setText(startPerDayTextField);
    }

    public JRadioButton getRunPerWeekRadioButton() {
        return runPerWeekRadioButton;
    }

    public void setRunPerWeekRadioButton(boolean runPerWeek) {
        this.runPerWeekRadioButton.setSelected(runPerWeek);
    }

    public JButton getScheduleSaveButton() {
        return scheduleSaveButton;
    }

    public void setScheduleSaveButton(JButton scheduleSaveButton) {
        this.scheduleSaveButton = scheduleSaveButton;
    }

    public JComboBox getSchedulePerWeekComboBox() {
        return schedulePerWeekComboBox;
    }

    public void setSchedulePerWeekComboBox(String schedulePerWeek) {
        this.schedulePerWeekComboBox.setSelectedItem(schedulePerWeek);
    }

    public JTextField getStartPerWeekTextField() {
        return startPerWeekTextField;
    }

    public void setStartPerWeekTextField(String startPerWeekTextField) {
        this.startPerWeekTextField.setText(startPerWeekTextField);
    }

    public JTextField getWechatAppIdTextField() {
        return wechatAppIdTextField;
    }

    public void setWechatAppIdTextField(String wechatAppIdTextField) {
        this.wechatAppIdTextField.setText(wechatAppIdTextField);
    }

    public JPasswordField getWechatAppSecretPasswordField() {
        return wechatAppSecretPasswordField;
    }

    public void setWechatAppSecretPasswordField(String wechatAppSecretPasswordField) {
        this.wechatAppSecretPasswordField.setText(wechatAppSecretPasswordField);
    }

    public JPasswordField getWechatTokenPasswordField() {
        return wechatTokenPasswordField;
    }

    public void setWechatTokenPasswordField(String wechatTokenPasswordField) {
        this.wechatTokenPasswordField.setText(wechatTokenPasswordField);
    }

    public JPasswordField getWechatAesKeyPasswordField() {
        return wechatAesKeyPasswordField;
    }

    public void setWechatAesKeyPasswordField(String wechatAesKeyPasswordField) {
        this.wechatAesKeyPasswordField.setText(wechatAesKeyPasswordField);
    }

    public JButton getSettingMpInfoSaveButton() {
        return settingMpInfoSaveButton;
    }

    public void setSettingMpInfoSaveButton(JButton settingMpInfoSaveButton) {
        this.settingMpInfoSaveButton = settingMpInfoSaveButton;
    }

    public JTextField getAliServerUrlTextField() {
        return aliServerUrlTextField;
    }

    public void setAliServerUrlTextField(String aliServerUrlTextField) {
        this.aliServerUrlTextField.setText(aliServerUrlTextField);
    }

    public JPasswordField getAliAppKeyPasswordField() {
        return aliAppKeyPasswordField;
    }

    public void setAliAppKeyPasswordField(String aliAppKeyPasswordField) {
        this.aliAppKeyPasswordField.setText(aliAppKeyPasswordField);
    }

    public JTextField getAliSignTextField() {
        return aliSignTextField;
    }

    public void setAliSignTextField(String aliSignTextField) {
        this.aliSignTextField.setText(aliSignTextField);
    }

    public JButton getSettingAliInfoSaveButton() {
        return settingAliInfoSaveButton;
    }

    public void setSettingAliInfoSaveButton(JButton settingAliInfoSaveButton) {
        this.settingAliInfoSaveButton = settingAliInfoSaveButton;
    }

    public JTextField getMysqlUrlTextField() {
        return mysqlUrlTextField;
    }

    public void setMysqlUrlTextField(String mysqlUrlTextField) {
        this.mysqlUrlTextField.setText(mysqlUrlTextField);
    }

    public JTextField getMysqlDatabaseTextField() {
        return mysqlDatabaseTextField;
    }

    public void setMysqlDatabaseTextField(String mysqlDatabaseTextField) {
        this.mysqlDatabaseTextField.setText(mysqlDatabaseTextField);
    }

    public JTextField getMysqlUserTextField() {
        return mysqlUserTextField;
    }

    public void setMysqlUserTextField(String mysqlUserTextField) {
        this.mysqlUserTextField.setText(mysqlUserTextField);
    }

    public JPasswordField getMysqlPasswordField() {
        return mysqlPasswordField;
    }

    public void setMysqlPasswordField(String mysqlPasswordField) {
        this.mysqlPasswordField.setText(mysqlPasswordField);
    }

    public JButton getSettingTestDbLinkButton() {
        return settingTestDbLinkButton;
    }

    public void setSettingTestDbLinkButton(JButton settingTestDbLinkButton) {
        this.settingTestDbLinkButton = settingTestDbLinkButton;
    }

    public JButton getSettingDbInfoSaveButton() {
        return settingDbInfoSaveButton;
    }

    public void setSettingDbInfoSaveButton(JButton settingDbInfoSaveButton) {
        this.settingDbInfoSaveButton = settingDbInfoSaveButton;
    }

    public JTable getMsgHistable() {
        return msgHistable;
    }

    public void setMsgHistable(JTable msgHistable) {
        this.msgHistable = msgHistable;
    }

    public JCheckBox getDryRunCheckBox() {
        return dryRunCheckBox;
    }

    public void setDryRunCheckBox(boolean dryRunCheck) {
        this.dryRunCheckBox.setSelected(dryRunCheck);
    }

    public JLabel getSloganLabel() {
        return sloganLabel;
    }

    public void setSloganLabel(JLabel sloganLabel) {
        this.sloganLabel = sloganLabel;
    }

    public void setPreviewUserField(JTextField previewUserField) {
        this.previewUserField = previewUserField;
    }

    public void setImportFromSqlTextArea(JTextArea importFromSqlTextArea) {
        this.importFromSqlTextArea = importFromSqlTextArea;
    }

    public void setMemberFilePathField(JTextField memberFilePathField) {
        this.memberFilePathField = memberFilePathField;
    }

    public void setMemberTabCountLabel(JLabel memberTabCountLabel) {
        this.memberTabCountLabel = memberTabCountLabel;
    }

    public void setMsgNameField(JTextField msgNameField) {
        this.msgNameField = msgNameField;
    }

    public JLabel getCheckUpdateLabel() {
        return checkUpdateLabel;
    }

    public void setCheckUpdateLabel(JLabel checkUpdateLabel) {
        this.checkUpdateLabel = checkUpdateLabel;
    }

    public JButton getCreateMsgButton() {
        return createMsgButton;
    }

    public void setCreateMsgButton(JButton createMsgButton) {
        this.createMsgButton = createMsgButton;
    }

    public JButton getMemberImportAllButton() {
        return memberImportAllButton;
    }

    public void setMemberImportAllButton(JButton memberImportAllButton) {
        this.memberImportAllButton = memberImportAllButton;
    }

    public JComboBox getSettingThemeComboBox() {
        return settingThemeComboBox;
    }

    public void setSettingThemeComboBox(String settingThemeComboBox) {
        this.settingThemeComboBox.setSelectedItem(settingThemeComboBox);
    }

    public JComboBox getSettingFontNameComboBox() {
        return settingFontNameComboBox;
    }

    public void setSettingFontNameComboBox(String settingFontNameComboBox) {
        this.settingFontNameComboBox.setSelectedItem(settingFontNameComboBox);
    }

    public JComboBox getSettingFontSizeComboBox() {
        return settingFontSizeComboBox;
    }

    public void setSettingFontSizeComboBox(int settingFontSizeComboBox) {
        this.settingFontSizeComboBox.setSelectedItem(String.valueOf(settingFontSizeComboBox));
    }

    public JButton getSettingAppearanceSaveButton() {
        return settingAppearanceSaveButton;
    }

    public void setSettingAppearanceSaveButton(JButton settingAppearanceSaveButton) {
        this.settingAppearanceSaveButton = settingAppearanceSaveButton;
    }

    public JLabel getPushLastTimeLabel() {
        return pushLastTimeLabel;
    }

    public void setPushLastTimeLabel(JLabel pushLastTimeLabel) {
        this.pushLastTimeLabel = pushLastTimeLabel;
    }

    public JLabel getPushLeftTimeLabel() {
        return pushLeftTimeLabel;
    }

    public void setPushLeftTimeLabel(JLabel pushLeftTimeLabel) {
        this.pushLeftTimeLabel = pushLeftTimeLabel;
    }

    public JLabel getPushMsgName() {
        return pushMsgName;
    }

    public void setPushMsgName(String pushMsgName) {
        this.pushMsgName.setText(pushMsgName);
    }

    public JButton getPushPauseButton() {
        return pushPauseButton;
    }

    public void setPushPauseButton(JButton pushPauseButton) {
        this.pushPauseButton = pushPauseButton;
    }

    public JScrollPane getSettingScrollPane() {
        return settingScrollPane;
    }

    public void setSettingScrollPane(JScrollPane settingScrollPane) {
        this.settingScrollPane = settingScrollPane;
    }

    public JPasswordField getAliAppSecretPasswordField() {
        return aliAppSecretPasswordField;
    }

    public void setAliAppSecretPasswordField(String aliAppSecretPasswordField) {
        this.aliAppSecretPasswordField.setText(aliAppSecretPasswordField);
    }

    public JButton getImportFromHisButton() {
        return importFromHisButton;
    }

    public void setImportFromHisButton(JButton importFromHisButton) {
        this.importFromHisButton = importFromHisButton;
    }

    public JButton getClearImportButton() {
        return clearImportButton;
    }

    public void setClearImportButton(JButton clearImportButton) {
        this.clearImportButton = clearImportButton;
    }

    public JComboBox getMemberHisComboBox() {
        return memberHisComboBox;
    }

    public void setMemberHisComboBox(JComboBox memberHisComboBox) {
        this.memberHisComboBox = memberHisComboBox;
    }

    public JTable getImportHisTable() {
        return importHisTable;
    }

    public void setImportHisTable(JTable importHisTable) {
        this.importHisTable = importHisTable;
    }

    public JButton getMsgHisTableSelectAllButton() {
        return msgHisTableSelectAllButton;
    }

    public void setMsgHisTableSelectAllButton(JButton msgHisTableSelectAllButton) {
        this.msgHisTableSelectAllButton = msgHisTableSelectAllButton;
    }

    public JButton getMsgHisTableDeleteButton() {
        return msgHisTableDeleteButton;
    }

    public void setMsgHisTableDeleteButton(JButton msgHisTableDeleteButton) {
        this.msgHisTableDeleteButton = msgHisTableDeleteButton;
    }

    public JButton getImportHisTableSelectAllButton() {
        return importHisTableSelectAllButton;
    }

    public void setImportHisTableSelectAllButton(JButton importHisTableSelectAllButton) {
        this.importHisTableSelectAllButton = importHisTableSelectAllButton;
    }

    public JButton getImportHisTableDeleteButton() {
        return importHisTableDeleteButton;
    }

    public void setImportHisTableDeleteButton(JButton importHisTableDeleteButton) {
        this.importHisTableDeleteButton = importHisTableDeleteButton;
    }

    public JButton getMsgHisTableUnselectAllButton() {
        return msgHisTableUnselectAllButton;
    }

    public void setMsgHisTableUnselectAllButton(JButton msgHisTableUnselectAllButton) {
        this.msgHisTableUnselectAllButton = msgHisTableUnselectAllButton;
    }

    public JButton getImportHisTableUnselectAllButton() {
        return importHisTableUnselectAllButton;
    }

    public void setImportHisTableUnselectAllButton(JButton importHisTableUnselectAllButton) {
        this.importHisTableUnselectAllButton = importHisTableUnselectAllButton;
    }

}
