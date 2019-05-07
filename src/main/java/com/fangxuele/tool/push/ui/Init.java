package com.fangxuele.tool.push.ui;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alee.laf.WebLookAndFeel;
import com.fangxuele.tool.push.bean.UserCase;
import com.fangxuele.tool.push.logic.MessageTypeConsts;
import com.fangxuele.tool.push.logic.MsgHisManage;
import com.fangxuele.tool.push.ui.component.TableInCellButtonColumn;
import com.fangxuele.tool.push.ui.component.TableInCellCheckBoxRenderer;
import com.fangxuele.tool.push.ui.form.AboutForm;
import com.fangxuele.tool.push.ui.form.HelpForm;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MemberForm;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.MessageManageForm;
import com.fangxuele.tool.push.ui.form.PushForm;
import com.fangxuele.tool.push.ui.form.PushHisForm;
import com.fangxuele.tool.push.ui.form.ScheduleForm;
import com.fangxuele.tool.push.ui.form.SettingForm;
import com.fangxuele.tool.push.ui.form.UserCaseForm;
import com.fangxuele.tool.push.ui.listener.AboutListener;
import com.fangxuele.tool.push.util.ConfigUtil;
import com.fangxuele.tool.push.util.SystemUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.commons.lang3.StringUtils;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <pre>
 * 初始化类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/15.
 */
public class Init {

    private static final Log logger = LogFactory.get();

    /**
     * 配置文件管理器对象
     */
    public static ConfigUtil configer = ConfigUtil.getInstance();

    /**
     * 消息管理
     */
    public static MsgHisManage msgHisManager = MsgHisManage.getInstance();

    /**
     * 设置全局字体
     */
    public static void initGlobalFont() {

        // 低分辨率屏幕字号初始化
        String lowDpiKey = "lowDpiInit";
        // 得到屏幕的尺寸
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (screenSize.width <= 1366 && StringUtils.isEmpty(configer.getProps(lowDpiKey))) {
            configer.setFontSize(13);
            configer.setProps(lowDpiKey, "true");
            configer.save();
        }

        // Mac高分辨率屏幕字号初始化
        String highDpiKey = "highDpiInit";
        if (SystemUtil.isMacOs() && StringUtils.isEmpty(configer.getProps(highDpiKey))) {
            configer.setFontSize(15);
            configer.setProps(highDpiKey, "true");
            configer.save();
        }

        Font fnt = new Font(configer.getFont(), Font.PLAIN, configer.getFontSize());
        FontUIResource fontRes = new FontUIResource(fnt);
        for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, fontRes);
            }
        }
    }

    /**
     * 其他初始化
     */
    public static void initOthers() {
        // 设置滚动条速度
        SettingForm.settingForm.getSettingScrollPane().getVerticalScrollBar().setUnitIncrement(15);
        SettingForm.settingForm.getSettingScrollPane().getVerticalScrollBar().setDoubleBuffered(true);

        UserCaseForm.userCaseForm.getUserCaseScrollPane().getVerticalScrollBar().setUnitIncrement(15);
        UserCaseForm.userCaseForm.getUserCaseScrollPane().getVerticalScrollBar().setDoubleBuffered(true);

        // 设置版本
        AboutForm.aboutForm.getVersionLabel().setText(UiConsts.APP_VERSION);
    }

    /**
     * 初始化look and feel
     */
    public static void initTheme() {

        try {
            switch (configer.getTheme()) {
                case "BeautyEye":
                    BeautyEyeLNFHelper.launchBeautyEyeLNF();
                    UIManager.put("RootPane.setupButtonVisible", false);
                    break;
                case "weblaf":
                    UIManager.setLookAndFeel(new WebLookAndFeel());
                    break;
                case "系统默认":
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
                case "Darcula(推荐)":
                default:
                    UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");
            }
        } catch (Exception e) {
            logger.error(e);
        }

    }

    /**
     * 初始化使用帮助tab
     */
    private static void initHelpTab() {

        try {
            HelpForm.helpForm.getHelpTextPane().setEditable(false);
            HTMLEditorKit kit = new HTMLEditorKit();
            HelpForm.helpForm.getHelpTextPane().setEditorKit(kit);
            StyleSheet styleSheet = kit.getStyleSheet();
            styleSheet.addRule("h2{color:#FBC87A;}");
            HelpForm.helpForm.getHelpTextPane().setContentType("text/html; charset=utf-8");
            HelpForm.helpForm.getHelpTextPane().setPage(MainWindow.class.getResource("/page/help.html"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 初始化他们都在用tab
     */
    private static void initUserCaseTab() {
        // 从github获取用户案例相关信息
        String userCaseInfoContent = HttpUtil.get(UiConsts.USER_CASE_URL);
        if (StringUtils.isNotEmpty(userCaseInfoContent)) {
            List<UserCase> userCaseInfoList = JSONUtil.toList(JSONUtil.parseArray(userCaseInfoContent), UserCase.class);

            JPanel userCaseListPanel = UserCaseForm.userCaseForm.getUserCaseListPanel();
            int listSize = userCaseInfoList.size();
            userCaseListPanel.setLayout(new GridLayoutManager((int) Math.ceil(listSize / 2.0) + 1, 3, new Insets(0, 0, 0, 0), -1, -1));
            for (int i = 0; i < listSize; i++) {
                UserCase userCase = userCaseInfoList.get(i);
                JPanel userCasePanel = new JPanel();
                userCasePanel.setLayout(new GridLayoutManager(2, 2, new Insets(10, 10, 0, 0), -1, -1));

                JLabel qrCodeLabel = new JLabel();
                try {
                    URL url = new URL(userCase.getQrCodeUrl());
                    BufferedImage image = ImageIO.read(url);
                    qrCodeLabel.setIcon(new ImageIcon(image));
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error(e);
                }
                JLabel titleLabel = new JLabel();
                titleLabel.setText(userCase.getTitle());
                Font fnt = new Font(configer.getFont(), Font.BOLD, 20);
                titleLabel.setFont(fnt);
                JLabel descLabel = new JLabel();
                descLabel.setText(userCase.getDesc());

                userCasePanel.add(qrCodeLabel, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
                userCasePanel.add(titleLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
                userCasePanel.add(descLabel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
                userCaseListPanel.add(userCasePanel, new GridConstraints(i / 2, i % 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
            }

            final Spacer spacer1 = new Spacer();
            userCaseListPanel.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
            final Spacer spacer2 = new Spacer();
            userCaseListPanel.add(spacer2, new GridConstraints((int) Math.ceil(listSize / 2.0), 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));

            userCaseListPanel.updateUI();
        }
    }

    /**
     * 初始化消息tab
     */
    public static void initMsgTab(String selectedMsgName) {
        // 初始化，清空所有相关的输入框内容
        MessageEditForm.messageEditForm.getMsgTypeComboBox().setSelectedItem("");
        MessageEditForm.messageEditForm.getMsgTemplateIdTextField().setText("");
        MessageEditForm.messageEditForm.getMsgTemplateUrlTextField().setText("");
        MessageEditForm.messageEditForm.getMsgKefuMsgTypeComboBox().setSelectedItem("");
        MessageEditForm.messageEditForm.getMsgKefuMsgTitleTextField().setText("");
        MessageEditForm.messageEditForm.getMsgKefuPicUrlTextField().setText("");
        MessageEditForm.messageEditForm.getMsgKefuDescTextField().setText("");
        MessageEditForm.messageEditForm.getMsgKefuUrlTextField().setText("");
        MessageEditForm.messageEditForm.getMsgTemplateMiniAppidTextField().setText("");
        MessageEditForm.messageEditForm.getMsgTemplateMiniPagePathTextField().setText("");
        MessageEditForm.messageEditForm.getMsgTemplateKeyWordTextField().setText("");
        MessageEditForm.messageEditForm.getMsgYunpianMsgContentTextField().setText("");

        String msgName;
        if (StringUtils.isEmpty(selectedMsgName)) {
            msgName = configer.getMsgName();
        } else {
            msgName = selectedMsgName;
        }

        MessageEditForm.messageEditForm.getMsgNameField().setText(msgName);
        MessageEditForm.messageEditForm.getPreviewUserField().setText(configer.getPreviewUser());

        Map<String, String[]> msgMap = msgHisManager.readMsgHis();

        if (msgMap != null && msgMap.size() != 0) {
            if (msgMap.containsKey(msgName)) {
                String[] msgDataArray = msgMap.get(msgName);
                String msgType = msgDataArray[1];
                MessageEditForm.messageEditForm.getMsgTypeComboBox().setSelectedItem(msgType);
                MessageEditForm.messageEditForm.getMsgTemplateIdTextField().setText(msgDataArray[2]);
                MessageEditForm.messageEditForm.getMsgTemplateUrlTextField().setText(msgDataArray[3]);
                String kefuMsgType = msgDataArray[4];
                MessageEditForm.messageEditForm.getMsgKefuMsgTypeComboBox().setSelectedItem(kefuMsgType);
                MessageEditForm.messageEditForm.getMsgKefuMsgTitleTextField().setText(msgDataArray[5]);
                MessageEditForm.messageEditForm.getMsgKefuPicUrlTextField().setText(msgDataArray[6]);
                MessageEditForm.messageEditForm.getMsgKefuDescTextField().setText(msgDataArray[7]);
                MessageEditForm.messageEditForm.getMsgKefuUrlTextField().setText(msgDataArray[8]);
                if (msgDataArray.length > 12) {
                    MessageEditForm.messageEditForm.getMsgYunpianMsgContentTextField().setText(msgDataArray[12]);
                }
                if (msgDataArray.length > 11) {
                    MessageEditForm.messageEditForm.getMsgTemplateKeyWordTextField().setText(msgDataArray[11]);
                }
                if (msgDataArray.length > 9) {
                    MessageEditForm.messageEditForm.getMsgTemplateMiniAppidTextField().setText(msgDataArray[9]);
                    MessageEditForm.messageEditForm.getMsgTemplateMiniPagePathTextField().setText(msgDataArray[10]);
                }
                switchMsgType(msgType);
                switchKefuMsgType(kefuMsgType);

                // 模板消息Data表
                List<String[]> list = msgHisManager.readTemplateData(msgName);
                String[] headerNames = {"Name", "Value", "Color", "操作"};
                Object[][] cellData = new String[list.size()][headerNames.length];
                for (int i = 0; i < list.size(); i++) {
                    cellData[i] = list.get(i);
                }
                DefaultTableModel model = new DefaultTableModel(cellData, headerNames);
                MessageEditForm.messageEditForm.getTemplateMsgDataTable().setModel(model);
                MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().
                        getColumn(headerNames.length - 1).
                        setCellRenderer(new TableInCellButtonColumn(MessageEditForm.messageEditForm.getTemplateMsgDataTable(), headerNames.length - 1));
                MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().
                        getColumn(headerNames.length - 1).
                        setCellEditor(new TableInCellButtonColumn(MessageEditForm.messageEditForm.getTemplateMsgDataTable(), headerNames.length - 1));

                // 设置列宽
                MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(0).setPreferredWidth(150);
                MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(0).setMaxWidth(150);
                MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(2).setPreferredWidth(130);
                MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(2).setMaxWidth(130);
                MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(3).setPreferredWidth(130);
                MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(3).setMaxWidth(130);

                MessageEditForm.messageEditForm.getTemplateMsgDataTable().updateUI();
            }
        } else {
            switchMsgType(Objects.requireNonNull(MessageEditForm.messageEditForm.getMsgTypeComboBox().getSelectedItem()).toString());
        }
    }

    /**
     * 根据消息类型转换界面显示
     *
     * @param msgType
     */
    public static void switchMsgType(String msgType) {
        MessageEditForm.messageEditForm.getKefuMsgPanel().setVisible(false);
        MessageEditForm.messageEditForm.getTemplateMsgPanel().setVisible(false);
        MessageEditForm.messageEditForm.getYunpianMsgPanel().setVisible(false);
        switch (msgType) {
            case MessageTypeConsts.MP_TEMPLATE:
                MessageEditForm.messageEditForm.getTemplateMsgPanel().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateUrlLabel().setVisible(true);
                MessageEditForm.messageEditForm.getMsgTemplateUrlTextField().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateMiniProgramAppidLabel().setVisible(true);
                MessageEditForm.messageEditForm.getMsgTemplateMiniAppidTextField().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateMiniProgramPagePathLabel().setVisible(true);
                MessageEditForm.messageEditForm.getMsgTemplateMiniPagePathTextField().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateMiniProgramOptionalLabel1().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateMiniProgramOptionalLabel2().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateMsgColorLabel().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateDataColorTextField().setVisible(true);
                MessageEditForm.messageEditForm.getMsgTemplateKeyWordTextField().setVisible(false);
                MessageEditForm.messageEditForm.getTemplateKeyWordLabel().setVisible(false);
                MessageEditForm.messageEditForm.getPreviewMemberLabel().setText("预览消息用户openid（多个以半角分号分隔）");
                break;
            case MessageTypeConsts.MA_TEMPLATE:
                MessageEditForm.messageEditForm.getTemplateMsgPanel().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateUrlLabel().setVisible(true);
                MessageEditForm.messageEditForm.getMsgTemplateUrlTextField().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateMiniProgramAppidLabel().setVisible(false);
                MessageEditForm.messageEditForm.getMsgTemplateMiniAppidTextField().setVisible(false);
                MessageEditForm.messageEditForm.getTemplateMiniProgramPagePathLabel().setVisible(false);
                MessageEditForm.messageEditForm.getMsgTemplateMiniPagePathTextField().setVisible(false);
                MessageEditForm.messageEditForm.getTemplateMiniProgramOptionalLabel1().setVisible(false);
                MessageEditForm.messageEditForm.getTemplateMiniProgramOptionalLabel2().setVisible(false);
                MessageEditForm.messageEditForm.getTemplateMsgColorLabel().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateDataColorTextField().setVisible(true);
                MessageEditForm.messageEditForm.getMsgTemplateKeyWordTextField().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateKeyWordLabel().setVisible(true);
                MessageEditForm.messageEditForm.getPreviewMemberLabel().setText("预览消息用户openid（多个以半角分号分隔）");
                break;
            case MessageTypeConsts.KEFU:
                MessageEditForm.messageEditForm.getKefuMsgPanel().setVisible(true);
                MessageEditForm.messageEditForm.getPreviewMemberLabel().setText("预览消息用户openid（多个以半角分号分隔）");
                break;
            case MessageTypeConsts.KEFU_PRIORITY:
                MessageEditForm.messageEditForm.getKefuMsgPanel().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateMsgPanel().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateUrlLabel().setVisible(true);
                MessageEditForm.messageEditForm.getMsgTemplateUrlTextField().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateMiniProgramAppidLabel().setVisible(true);
                MessageEditForm.messageEditForm.getMsgTemplateMiniAppidTextField().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateMiniProgramPagePathLabel().setVisible(true);
                MessageEditForm.messageEditForm.getMsgTemplateMiniPagePathTextField().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateMiniProgramOptionalLabel1().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateMiniProgramOptionalLabel2().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateMsgColorLabel().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateDataColorTextField().setVisible(true);
                MessageEditForm.messageEditForm.getMsgTemplateKeyWordTextField().setVisible(false);
                MessageEditForm.messageEditForm.getTemplateKeyWordLabel().setVisible(false);
                MessageEditForm.messageEditForm.getPreviewMemberLabel().setText("预览消息用户openid（多个以半角分号分隔）");
                break;
            case MessageTypeConsts.ALI_YUN:
            case MessageTypeConsts.TX_YUN:
            case MessageTypeConsts.ALI_TEMPLATE:
                MessageEditForm.messageEditForm.getTemplateMsgPanel().setVisible(true);
                MessageEditForm.messageEditForm.getTemplateUrlLabel().setVisible(false);
                MessageEditForm.messageEditForm.getMsgTemplateUrlTextField().setVisible(false);
                MessageEditForm.messageEditForm.getTemplateMiniProgramAppidLabel().setVisible(false);
                MessageEditForm.messageEditForm.getMsgTemplateMiniAppidTextField().setVisible(false);
                MessageEditForm.messageEditForm.getTemplateMiniProgramPagePathLabel().setVisible(false);
                MessageEditForm.messageEditForm.getMsgTemplateMiniPagePathTextField().setVisible(false);
                MessageEditForm.messageEditForm.getTemplateMiniProgramOptionalLabel1().setVisible(false);
                MessageEditForm.messageEditForm.getTemplateMiniProgramOptionalLabel2().setVisible(false);
                MessageEditForm.messageEditForm.getTemplateMsgColorLabel().setVisible(false);
                MessageEditForm.messageEditForm.getTemplateDataColorTextField().setVisible(false);
                MessageEditForm.messageEditForm.getMsgTemplateKeyWordTextField().setVisible(false);
                MessageEditForm.messageEditForm.getTemplateKeyWordLabel().setVisible(false);
                MessageEditForm.messageEditForm.getPreviewMemberLabel().setText("预览消息用户手机号（多个以半角分号分隔）");
                break;
            case MessageTypeConsts.YUN_PIAN:
                MessageEditForm.messageEditForm.getYunpianMsgPanel().setVisible(true);
                MessageEditForm.messageEditForm.getPreviewMemberLabel().setText("预览消息用户手机号（多个以半角分号分隔）");
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
                MessageEditForm.messageEditForm.getKefuMsgTitleLabel().setText("内容");
                MessageEditForm.messageEditForm.getKefuMsgDescLabel().setVisible(false);
                MessageEditForm.messageEditForm.getMsgKefuDescTextField().setVisible(false);
                MessageEditForm.messageEditForm.getKefuMsgPicUrlLabel().setVisible(false);
                MessageEditForm.messageEditForm.getMsgKefuPicUrlTextField().setVisible(false);
                MessageEditForm.messageEditForm.getMsgKefuDescTextField().setVisible(false);
                MessageEditForm.messageEditForm.getKefuMsgUrlLabel().setVisible(false);
                MessageEditForm.messageEditForm.getMsgKefuUrlTextField().setVisible(false);
                break;
            case "图文消息":
                MessageEditForm.messageEditForm.getKefuMsgTitleLabel().setText("标题");
                MessageEditForm.messageEditForm.getKefuMsgDescLabel().setVisible(true);
                MessageEditForm.messageEditForm.getMsgKefuDescTextField().setVisible(true);
                MessageEditForm.messageEditForm.getKefuMsgPicUrlLabel().setVisible(true);
                MessageEditForm.messageEditForm.getMsgKefuPicUrlTextField().setVisible(true);
                MessageEditForm.messageEditForm.getMsgKefuDescTextField().setVisible(true);
                MessageEditForm.messageEditForm.getKefuMsgUrlLabel().setVisible(true);
                MessageEditForm.messageEditForm.getMsgKefuUrlTextField().setVisible(true);
                break;
            default:
                break;
        }
    }

    /**
     * 初始化导入用户tab
     */
    public static void initMemberTab() {
        MemberForm.memberForm.getImportFromSqlTextArea().setText(configer.getMemberSql());
        MemberForm.memberForm.getMemberFilePathField().setText(configer.getMemberFilePath());

        MemberForm.memberForm.getMemberHisComboBox().removeAllItems();

        File pushHisDir = new File(SystemUtil.configHome + "data" + File.separator + "push_his");
        if (!pushHisDir.exists()) {
            pushHisDir.mkdirs();
        }

        File[] files = pushHisDir.listFiles();
        if (Objects.requireNonNull(files).length > 0) {
            for (File file : files) {
                MemberForm.memberForm.getMemberHisComboBox().addItem(file.getName());
            }
        }
    }

    /**
     * 初始化推送tab
     */
    private static void initPushTab() {
        PushForm.pushForm.getPushMsgName().setText(configer.getMsgName());
        PushForm.pushForm.getMaxThreadPoolTextField().setText(String.valueOf(configer.getMaxThreadPool()));
        PushForm.pushForm.getThreadCountTextField().setText(String.valueOf(configer.getThreadCount()));
        PushForm.pushForm.getDryRunCheckBox().setSelected(configer.isDryRun());
    }

    /**
     * 初始化计划任务tab
     */
    private static void initScheduleTab() {
        // 开始
        ScheduleForm.scheduleForm.getRunAtThisTimeRadioButton().setSelected(configer.isRadioStartAt());
        ScheduleForm.scheduleForm.getStartAtThisTimeTextField().setText(configer.getTextStartAt());

        //每天
        ScheduleForm.scheduleForm.getRunPerDayRadioButton().setSelected(configer.isRadioPerDay());
        ScheduleForm.scheduleForm.getStartPerDayTextField().setText(configer.getTextPerDay());

        // 每周
        ScheduleForm.scheduleForm.getRunPerWeekRadioButton().setSelected(configer.isRadioPerWeek());
        ScheduleForm.scheduleForm.getSchedulePerWeekComboBox().setSelectedItem(configer.getTextPerWeekWeek());
        ScheduleForm.scheduleForm.getStartPerWeekTextField().setText(configer.getTextPerWeekTime());
    }

    /**
     * 初始化推送历史tab
     */
    public static void initPushHisTab() {
        // 导入历史管理
        String[] headerNames = {"选择", "文件名称"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        PushHisForm.pushHisForm.getPushHisLeftTable().setModel(model);

        // 隐藏表头
        PushHisForm.pushHisForm.getPushHisLeftTable().getTableHeader().setVisible(false);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setPreferredSize(new Dimension(0, 0));
        PushHisForm.pushHisForm.getPushHisLeftTable().getTableHeader().setDefaultRenderer(renderer);

        File pushHisDir = new File(SystemUtil.configHome + "data" + File.separator + "push_his");
        if (!pushHisDir.exists()) {
            pushHisDir.mkdirs();
        }

        File[] files = pushHisDir.listFiles();
        Object[] data;
        if (Objects.requireNonNull(files).length > 0) {
            for (File file : files) {
                data = new Object[2];
                data[0] = false;
                data[1] = file.getName();
                model.addRow(data);
            }
        }
        PushHisForm.pushHisForm.getPushHisLeftTable().getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        PushHisForm.pushHisForm.getPushHisLeftTable().getColumnModel().getColumn(0).setCellRenderer(new TableInCellCheckBoxRenderer());
        // 设置列宽
        PushHisForm.pushHisForm.getPushHisLeftTable().getColumnModel().getColumn(0).setPreferredWidth(30);
        PushHisForm.pushHisForm.getPushHisLeftTable().getColumnModel().getColumn(0).setMaxWidth(30);
    }

    /**
     * 初始化设置tab
     */
    public static void initSettingTab() {
        // 常规
        SettingForm.settingForm.getAutoCheckUpdateCheckBox().setSelected(configer.isAutoCheckUpdate());

        // 微信公众号
        SettingForm.settingForm.getWechatAppIdTextField().setText(configer.getWechatAppId());
        SettingForm.settingForm.getWechatAppSecretPasswordField().setText(configer.getWechatAppSecret());
        SettingForm.settingForm.getWechatTokenPasswordField().setText(configer.getWechatToken());
        SettingForm.settingForm.getWechatAesKeyPasswordField().setText(configer.getWechatAesKey());

        // 微信小程序
        SettingForm.settingForm.getMiniAppAppIdTextField().setText(configer.getMiniAppAppId());
        SettingForm.settingForm.getMiniAppAppSecretPasswordField().setText(configer.getMiniAppAppSecret());
        SettingForm.settingForm.getMiniAppTokenPasswordField().setText(configer.getMiniAppToken());
        SettingForm.settingForm.getMiniAppAesKeyPasswordField().setText(configer.getMiniAppAesKey());

        // 阿里云短信
        SettingForm.settingForm.getAliyunAccessKeyIdTextField().setText(configer.getAliyunAccessKeyId());
        SettingForm.settingForm.getAliyunAccessKeySecretTextField().setText(configer.getAliyunAccessKeySecret());
        SettingForm.settingForm.getAliyunSignTextField().setText(configer.getAliyunSign());

        // 阿里大于
        SettingForm.settingForm.getAliServerUrlTextField().setText(configer.getAliServerUrl());
        SettingForm.settingForm.getAliAppKeyPasswordField().setText(configer.getAliAppKey());
        SettingForm.settingForm.getAliAppSecretPasswordField().setText(configer.getAliAppSecret());
        SettingForm.settingForm.getAliSignTextField().setText(configer.getAliSign());

        // 腾讯云短信
        SettingForm.settingForm.getTxyunAppIdTextField().setText(configer.getTxyunAppId());
        SettingForm.settingForm.getTxyunAppKeyTextField().setText(configer.getTxyunAppKey());
        SettingForm.settingForm.getTxyunSignTextField().setText(configer.getTxyunSign());

        // 云片网短信
        SettingForm.settingForm.getYunpianApiKeyTextField().setText(configer.getYunpianApiKey());

        // MySQL
        SettingForm.settingForm.getMysqlUrlTextField().setText(configer.getMysqlUrl());
        SettingForm.settingForm.getMysqlDatabaseTextField().setText(configer.getMysqlDatabase());
        SettingForm.settingForm.getMysqlUserTextField().setText(configer.getMysqlUser());
        SettingForm.settingForm.getMysqlPasswordField().setText(configer.getMysqlPassword());

        // 外观
        SettingForm.settingForm.getSettingThemeComboBox().setSelectedItem(configer.getTheme());
        SettingForm.settingForm.getSettingFontNameComboBox().setSelectedItem(configer.getFont());
        SettingForm.settingForm.getSettingFontSizeComboBox().setSelectedItem(String.valueOf(configer.getFontSize()));

        // 历史消息管理
        String[] headerNames = {"选择", "消息名称"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        MessageManageForm.messageManageForm.getMsgHistable().setModel(model);
        // 隐藏表头
        MessageManageForm.messageManageForm.getMsgHistable().getTableHeader().setVisible(false);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setPreferredSize(new Dimension(0, 0));
        MessageManageForm.messageManageForm.getMsgHistable().getTableHeader().setDefaultRenderer(renderer);

        Map<String, String[]> msgMap = msgHisManager.readMsgHis();
        Object[] data;
        for (String msgName : msgMap.keySet()) {
            data = new Object[2];
            data[0] = false;
            data[1] = msgName;
            model.addRow(data);
        }
        MessageManageForm.messageManageForm.getMsgHistable().getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        MessageManageForm.messageManageForm.getMsgHistable().getColumnModel().getColumn(0).setCellRenderer(new TableInCellCheckBoxRenderer());
        // 设置列宽
        MessageManageForm.messageManageForm.getMsgHistable().getColumnModel().getColumn(0).setPreferredWidth(50);
        MessageManageForm.messageManageForm.getMsgHistable().getColumnModel().getColumn(0).setMaxWidth(50);
    }

    /**
     * 初始化模板消息数据table
     */
    public static void initTemplateDataTable() {
        String[] headerNames = {"Name", "Value", "Color", "操作"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        MessageEditForm.messageEditForm.getTemplateMsgDataTable().setModel(model);
        MessageEditForm.messageEditForm.getTemplateMsgDataTable().updateUI();
        MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().
                getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(MessageEditForm.messageEditForm.getTemplateMsgDataTable(), headerNames.length - 1));
        MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().
                getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(MessageEditForm.messageEditForm.getTemplateMsgDataTable(), headerNames.length - 1));

        // 设置列宽
        MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(0).setPreferredWidth(150);
        MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(0).setMaxWidth(150);
        MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(2).setPreferredWidth(130);
        MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(2).setMaxWidth(130);
        MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(3).setPreferredWidth(130);
        MessageEditForm.messageEditForm.getTemplateMsgDataTable().getColumnModel().getColumn(3).setMaxWidth(130);
    }

    /**
     * 初始化所有tab
     */
    public static void initAllTab() {
        initHelpTab();
        ThreadUtil.execute(Init::initUserCaseTab);
        initMsgTab(null);
        initMemberTab();
        initPushTab();
        initScheduleTab();
        // 初始化后置，切换tab时再触发
        // initPushHisTab();
        initSettingTab();

        // 检查新版版
        if (configer.isAutoCheckUpdate()) {
            ThreadUtil.execute(() -> AboutListener.checkUpdate(true));
        }
        // 更新二维码
        ThreadUtil.execute(AboutListener::initQrCode);
    }

}
