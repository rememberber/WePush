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
import com.fangxuele.tool.push.ui.form.ScheduleForm;
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
        MainWindow.mainWindow.getSettingScrollPane().getVerticalScrollBar().setUnitIncrement(15);
        MainWindow.mainWindow.getSettingScrollPane().getVerticalScrollBar().setDoubleBuffered(true);

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
        MainWindow.mainWindow.getMsgTypeComboBox().setSelectedItem("");
        MainWindow.mainWindow.getMsgTemplateIdTextField().setText("");
        MainWindow.mainWindow.getMsgTemplateUrlTextField().setText("");
        MainWindow.mainWindow.getMsgKefuMsgTypeComboBox().setSelectedItem("");
        MainWindow.mainWindow.getMsgKefuMsgTitleTextField().setText("");
        MainWindow.mainWindow.getMsgKefuPicUrlTextField().setText("");
        MainWindow.mainWindow.getMsgKefuDescTextField().setText("");
        MainWindow.mainWindow.getMsgKefuUrlTextField().setText("");
        MainWindow.mainWindow.getMsgTemplateMiniAppidTextField().setText("");
        MainWindow.mainWindow.getMsgTemplateMiniPagePathTextField().setText("");
        MainWindow.mainWindow.getMsgTemplateKeyWordTextField().setText("");
        MainWindow.mainWindow.getMsgYunpianMsgContentTextField().setText("");

        String msgName;
        if (StringUtils.isEmpty(selectedMsgName)) {
            msgName = configer.getMsgName();
        } else {
            msgName = selectedMsgName;
        }

        MainWindow.mainWindow.getMsgNameField().setText(msgName);
        MainWindow.mainWindow.getPreviewUserField().setText(configer.getPreviewUser());

        Map<String, String[]> msgMap = msgHisManager.readMsgHis();

        if (msgMap != null && msgMap.size() != 0) {
            if (msgMap.containsKey(msgName)) {
                String[] msgDataArray = msgMap.get(msgName);
                String msgType = msgDataArray[1];
                MainWindow.mainWindow.getMsgTypeComboBox().setSelectedItem(msgType);
                MainWindow.mainWindow.getMsgTemplateIdTextField().setText(msgDataArray[2]);
                MainWindow.mainWindow.getMsgTemplateUrlTextField().setText(msgDataArray[3]);
                String kefuMsgType = msgDataArray[4];
                MainWindow.mainWindow.getMsgKefuMsgTypeComboBox().setSelectedItem(kefuMsgType);
                MainWindow.mainWindow.getMsgKefuMsgTitleTextField().setText(msgDataArray[5]);
                MainWindow.mainWindow.getMsgKefuPicUrlTextField().setText(msgDataArray[6]);
                MainWindow.mainWindow.getMsgKefuDescTextField().setText(msgDataArray[7]);
                MainWindow.mainWindow.getMsgKefuUrlTextField().setText(msgDataArray[8]);
                if (msgDataArray.length > 12) {
                    MainWindow.mainWindow.getMsgYunpianMsgContentTextField().setText(msgDataArray[12]);
                }
                if (msgDataArray.length > 11) {
                    MainWindow.mainWindow.getMsgTemplateKeyWordTextField().setText(msgDataArray[11]);
                }
                if (msgDataArray.length > 9) {
                    MainWindow.mainWindow.getMsgTemplateMiniAppidTextField().setText(msgDataArray[9]);
                    MainWindow.mainWindow.getMsgTemplateMiniPagePathTextField().setText(msgDataArray[10]);
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
                MainWindow.mainWindow.getTemplateMsgDataTable().setModel(model);
                MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().
                        getColumn(headerNames.length - 1).
                        setCellRenderer(new TableInCellButtonColumn(MainWindow.mainWindow.getTemplateMsgDataTable(), headerNames.length - 1));
                MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().
                        getColumn(headerNames.length - 1).
                        setCellEditor(new TableInCellButtonColumn(MainWindow.mainWindow.getTemplateMsgDataTable(), headerNames.length - 1));

                // 设置列宽
                MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().getColumn(0).setPreferredWidth(150);
                MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().getColumn(0).setMaxWidth(150);
                MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().getColumn(2).setPreferredWidth(130);
                MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().getColumn(2).setMaxWidth(130);
                MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().getColumn(3).setPreferredWidth(130);
                MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().getColumn(3).setMaxWidth(130);

                MainWindow.mainWindow.getTemplateMsgDataTable().updateUI();
            }
        } else {
            switchMsgType(Objects.requireNonNull(MainWindow.mainWindow.getMsgTypeComboBox().getSelectedItem()).toString());
        }
    }

    /**
     * 根据消息类型转换界面显示
     *
     * @param msgType
     */
    public static void switchMsgType(String msgType) {
        MainWindow.mainWindow.getKefuMsgPanel().setVisible(false);
        MainWindow.mainWindow.getTemplateMsgPanel().setVisible(false);
        MainWindow.mainWindow.getYunpianMsgPanel().setVisible(false);
        switch (msgType) {
            case MessageTypeConsts.MP_TEMPLATE:
                MainWindow.mainWindow.getTemplateMsgPanel().setVisible(true);
                MainWindow.mainWindow.getTemplateUrlLabel().setVisible(true);
                MainWindow.mainWindow.getMsgTemplateUrlTextField().setVisible(true);
                MainWindow.mainWindow.getTemplateMiniProgramAppidLabel().setVisible(true);
                MainWindow.mainWindow.getMsgTemplateMiniAppidTextField().setVisible(true);
                MainWindow.mainWindow.getTemplateMiniProgramPagePathLabel().setVisible(true);
                MainWindow.mainWindow.getMsgTemplateMiniPagePathTextField().setVisible(true);
                MainWindow.mainWindow.getTemplateMiniProgramOptionalLabel1().setVisible(true);
                MainWindow.mainWindow.getTemplateMiniProgramOptionalLabel2().setVisible(true);
                MainWindow.mainWindow.getTemplateMsgColorLabel().setVisible(true);
                MainWindow.mainWindow.getTemplateDataColorTextField().setVisible(true);
                MainWindow.mainWindow.getMsgTemplateKeyWordTextField().setVisible(false);
                MainWindow.mainWindow.getTemplateKeyWordLabel().setVisible(false);
                MainWindow.mainWindow.getPreviewMemberLabel().setText("预览消息用户openid（多个以半角分号分隔）");
                break;
            case MessageTypeConsts.MA_TEMPLATE:
                MainWindow.mainWindow.getTemplateMsgPanel().setVisible(true);
                MainWindow.mainWindow.getTemplateUrlLabel().setVisible(true);
                MainWindow.mainWindow.getMsgTemplateUrlTextField().setVisible(true);
                MainWindow.mainWindow.getTemplateMiniProgramAppidLabel().setVisible(false);
                MainWindow.mainWindow.getMsgTemplateMiniAppidTextField().setVisible(false);
                MainWindow.mainWindow.getTemplateMiniProgramPagePathLabel().setVisible(false);
                MainWindow.mainWindow.getMsgTemplateMiniPagePathTextField().setVisible(false);
                MainWindow.mainWindow.getTemplateMiniProgramOptionalLabel1().setVisible(false);
                MainWindow.mainWindow.getTemplateMiniProgramOptionalLabel2().setVisible(false);
                MainWindow.mainWindow.getTemplateMsgColorLabel().setVisible(true);
                MainWindow.mainWindow.getTemplateDataColorTextField().setVisible(true);
                MainWindow.mainWindow.getMsgTemplateKeyWordTextField().setVisible(true);
                MainWindow.mainWindow.getTemplateKeyWordLabel().setVisible(true);
                MainWindow.mainWindow.getPreviewMemberLabel().setText("预览消息用户openid（多个以半角分号分隔）");
                break;
            case MessageTypeConsts.KEFU:
                MainWindow.mainWindow.getKefuMsgPanel().setVisible(true);
                MainWindow.mainWindow.getPreviewMemberLabel().setText("预览消息用户openid（多个以半角分号分隔）");
                break;
            case MessageTypeConsts.KEFU_PRIORITY:
                MainWindow.mainWindow.getKefuMsgPanel().setVisible(true);
                MainWindow.mainWindow.getTemplateMsgPanel().setVisible(true);
                MainWindow.mainWindow.getTemplateUrlLabel().setVisible(true);
                MainWindow.mainWindow.getMsgTemplateUrlTextField().setVisible(true);
                MainWindow.mainWindow.getTemplateMiniProgramAppidLabel().setVisible(true);
                MainWindow.mainWindow.getMsgTemplateMiniAppidTextField().setVisible(true);
                MainWindow.mainWindow.getTemplateMiniProgramPagePathLabel().setVisible(true);
                MainWindow.mainWindow.getMsgTemplateMiniPagePathTextField().setVisible(true);
                MainWindow.mainWindow.getTemplateMiniProgramOptionalLabel1().setVisible(true);
                MainWindow.mainWindow.getTemplateMiniProgramOptionalLabel2().setVisible(true);
                MainWindow.mainWindow.getTemplateMsgColorLabel().setVisible(true);
                MainWindow.mainWindow.getTemplateDataColorTextField().setVisible(true);
                MainWindow.mainWindow.getMsgTemplateKeyWordTextField().setVisible(false);
                MainWindow.mainWindow.getTemplateKeyWordLabel().setVisible(false);
                MainWindow.mainWindow.getPreviewMemberLabel().setText("预览消息用户openid（多个以半角分号分隔）");
                break;
            case MessageTypeConsts.ALI_YUN:
            case MessageTypeConsts.TX_YUN:
            case MessageTypeConsts.ALI_TEMPLATE:
                MainWindow.mainWindow.getTemplateMsgPanel().setVisible(true);
                MainWindow.mainWindow.getTemplateUrlLabel().setVisible(false);
                MainWindow.mainWindow.getMsgTemplateUrlTextField().setVisible(false);
                MainWindow.mainWindow.getTemplateMiniProgramAppidLabel().setVisible(false);
                MainWindow.mainWindow.getMsgTemplateMiniAppidTextField().setVisible(false);
                MainWindow.mainWindow.getTemplateMiniProgramPagePathLabel().setVisible(false);
                MainWindow.mainWindow.getMsgTemplateMiniPagePathTextField().setVisible(false);
                MainWindow.mainWindow.getTemplateMiniProgramOptionalLabel1().setVisible(false);
                MainWindow.mainWindow.getTemplateMiniProgramOptionalLabel2().setVisible(false);
                MainWindow.mainWindow.getTemplateMsgColorLabel().setVisible(false);
                MainWindow.mainWindow.getTemplateDataColorTextField().setVisible(false);
                MainWindow.mainWindow.getMsgTemplateKeyWordTextField().setVisible(false);
                MainWindow.mainWindow.getTemplateKeyWordLabel().setVisible(false);
                MainWindow.mainWindow.getPreviewMemberLabel().setText("预览消息用户手机号（多个以半角分号分隔）");
                break;
            case MessageTypeConsts.YUN_PIAN:
                MainWindow.mainWindow.getYunpianMsgPanel().setVisible(true);
                MainWindow.mainWindow.getPreviewMemberLabel().setText("预览消息用户手机号（多个以半角分号分隔）");
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
                MainWindow.mainWindow.getKefuMsgTitleLabel().setText("内容");
                MainWindow.mainWindow.getKefuMsgDescLabel().setVisible(false);
                MainWindow.mainWindow.getMsgKefuDescTextField().setVisible(false);
                MainWindow.mainWindow.getKefuMsgPicUrlLabel().setVisible(false);
                MainWindow.mainWindow.getMsgKefuPicUrlTextField().setVisible(false);
                MainWindow.mainWindow.getMsgKefuDescTextField().setVisible(false);
                MainWindow.mainWindow.getKefuMsgUrlLabel().setVisible(false);
                MainWindow.mainWindow.getMsgKefuUrlTextField().setVisible(false);
                break;
            case "图文消息":
                MainWindow.mainWindow.getKefuMsgTitleLabel().setText("标题");
                MainWindow.mainWindow.getKefuMsgDescLabel().setVisible(true);
                MainWindow.mainWindow.getMsgKefuDescTextField().setVisible(true);
                MainWindow.mainWindow.getKefuMsgPicUrlLabel().setVisible(true);
                MainWindow.mainWindow.getMsgKefuPicUrlTextField().setVisible(true);
                MainWindow.mainWindow.getMsgKefuDescTextField().setVisible(true);
                MainWindow.mainWindow.getKefuMsgUrlLabel().setVisible(true);
                MainWindow.mainWindow.getMsgKefuUrlTextField().setVisible(true);
                break;
            default:
                break;
        }
    }

    /**
     * 初始化导入用户tab
     */
    public static void initMemberTab() {
        MainWindow.mainWindow.getImportFromSqlTextArea().setText(configer.getMemberSql());
        MainWindow.mainWindow.getMemberFilePathField().setText(configer.getMemberFilePath());

        MainWindow.mainWindow.getMemberHisComboBox().removeAllItems();

        File pushHisDir = new File(SystemUtil.configHome + "data" + File.separator + "push_his");
        if (!pushHisDir.exists()) {
            pushHisDir.mkdirs();
        }

        File[] files = pushHisDir.listFiles();
        if (Objects.requireNonNull(files).length > 0) {
            for (File file : files) {
                MainWindow.mainWindow.getMemberHisComboBox().addItem(file.getName());
            }
        }
    }

    /**
     * 初始化推送tab
     */
    private static void initPushTab() {
        MainWindow.mainWindow.getPushMsgName().setText(configer.getMsgName());
        MainWindow.mainWindow.getMaxThreadPoolTextField().setText(String.valueOf(configer.getMaxThreadPool()));
        MainWindow.mainWindow.getThreadCountTextField().setText(String.valueOf(configer.getThreadCount()));
        MainWindow.mainWindow.getDryRunCheckBox().setSelected(configer.isDryRun());
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
        MainWindow.mainWindow.getPushHisLeftTable().setModel(model);

        // 隐藏表头
        MainWindow.mainWindow.getPushHisLeftTable().getTableHeader().setVisible(false);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setPreferredSize(new Dimension(0, 0));
        MainWindow.mainWindow.getPushHisLeftTable().getTableHeader().setDefaultRenderer(renderer);

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
        MainWindow.mainWindow.getPushHisLeftTable().getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        MainWindow.mainWindow.getPushHisLeftTable().getColumnModel().getColumn(0).setCellRenderer(new TableInCellCheckBoxRenderer());
        // 设置列宽
        MainWindow.mainWindow.getPushHisLeftTable().getColumnModel().getColumn(0).setPreferredWidth(30);
        MainWindow.mainWindow.getPushHisLeftTable().getColumnModel().getColumn(0).setMaxWidth(30);
    }

    /**
     * 初始化设置tab
     */
    public static void initSettingTab() {
        // 常规
        MainWindow.mainWindow.getAutoCheckUpdateCheckBox().setSelected(configer.isAutoCheckUpdate());

        // 微信公众号
        MainWindow.mainWindow.getWechatAppIdTextField().setText(configer.getWechatAppId());
        MainWindow.mainWindow.getWechatAppSecretPasswordField().setText(configer.getWechatAppSecret());
        MainWindow.mainWindow.getWechatTokenPasswordField().setText(configer.getWechatToken());
        MainWindow.mainWindow.getWechatAesKeyPasswordField().setText(configer.getWechatAesKey());

        // 微信小程序
        MainWindow.mainWindow.getMiniAppAppIdTextField().setText(configer.getMiniAppAppId());
        MainWindow.mainWindow.getMiniAppAppSecretPasswordField().setText(configer.getMiniAppAppSecret());
        MainWindow.mainWindow.getMiniAppTokenPasswordField().setText(configer.getMiniAppToken());
        MainWindow.mainWindow.getMiniAppAesKeyPasswordField().setText(configer.getMiniAppAesKey());

        // 阿里云短信
        MainWindow.mainWindow.getAliyunAccessKeyIdTextField().setText(configer.getAliyunAccessKeyId());
        MainWindow.mainWindow.getAliyunAccessKeySecretTextField().setText(configer.getAliyunAccessKeySecret());
        MainWindow.mainWindow.getAliyunSignTextField().setText(configer.getAliyunSign());

        // 阿里大于
        MainWindow.mainWindow.getAliServerUrlTextField().setText(configer.getAliServerUrl());
        MainWindow.mainWindow.getAliAppKeyPasswordField().setText(configer.getAliAppKey());
        MainWindow.mainWindow.getAliAppSecretPasswordField().setText(configer.getAliAppSecret());
        MainWindow.mainWindow.getAliSignTextField().setText(configer.getAliSign());

        // 腾讯云短信
        MainWindow.mainWindow.getTxyunAppIdTextField().setText(configer.getTxyunAppId());
        MainWindow.mainWindow.getTxyunAppKeyTextField().setText(configer.getTxyunAppKey());
        MainWindow.mainWindow.getTxyunSignTextField().setText(configer.getTxyunSign());

        // 云片网短信
        MainWindow.mainWindow.getYunpianApiKeyTextField().setText(configer.getYunpianApiKey());

        // MySQL
        MainWindow.mainWindow.getMysqlUrlTextField().setText(configer.getMysqlUrl());
        MainWindow.mainWindow.getMysqlDatabaseTextField().setText(configer.getMysqlDatabase());
        MainWindow.mainWindow.getMysqlUserTextField().setText(configer.getMysqlUser());
        MainWindow.mainWindow.getMysqlPasswordField().setText(configer.getMysqlPassword());

        // 外观
        MainWindow.mainWindow.getSettingThemeComboBox().setSelectedItem(configer.getTheme());
        MainWindow.mainWindow.getSettingFontNameComboBox().setSelectedItem(configer.getFont());
        MainWindow.mainWindow.getSettingFontSizeComboBox().setSelectedItem(configer.getFontSize());

        // 历史消息管理
        String[] headerNames = {"选择", "消息名称"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        MainWindow.mainWindow.getMsgHistable().setModel(model);
        // 隐藏表头
        MainWindow.mainWindow.getMsgHistable().getTableHeader().setVisible(false);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setPreferredSize(new Dimension(0, 0));
        MainWindow.mainWindow.getMsgHistable().getTableHeader().setDefaultRenderer(renderer);

        Map<String, String[]> msgMap = msgHisManager.readMsgHis();
        Object[] data;
        for (String msgName : msgMap.keySet()) {
            data = new Object[2];
            data[0] = false;
            data[1] = msgName;
            model.addRow(data);
        }
        MainWindow.mainWindow.getMsgHistable().getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        MainWindow.mainWindow.getMsgHistable().getColumnModel().getColumn(0).setCellRenderer(new TableInCellCheckBoxRenderer());
        // 设置列宽
        MainWindow.mainWindow.getMsgHistable().getColumnModel().getColumn(0).setPreferredWidth(50);
        MainWindow.mainWindow.getMsgHistable().getColumnModel().getColumn(0).setMaxWidth(50);
    }

    /**
     * 初始化模板消息数据table
     */
    public static void initTemplateDataTable() {
        String[] headerNames = {"Name", "Value", "Color", "操作"};
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        MainWindow.mainWindow.getTemplateMsgDataTable().setModel(model);
        MainWindow.mainWindow.getTemplateMsgDataTable().updateUI();
        MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().
                getColumn(headerNames.length - 1).
                setCellRenderer(new TableInCellButtonColumn(MainWindow.mainWindow.getTemplateMsgDataTable(), headerNames.length - 1));
        MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().
                getColumn(headerNames.length - 1).
                setCellEditor(new TableInCellButtonColumn(MainWindow.mainWindow.getTemplateMsgDataTable(), headerNames.length - 1));

        // 设置列宽
        MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().getColumn(0).setPreferredWidth(150);
        MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().getColumn(0).setMaxWidth(150);
        MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().getColumn(2).setPreferredWidth(130);
        MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().getColumn(2).setMaxWidth(130);
        MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().getColumn(3).setPreferredWidth(130);
        MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().getColumn(3).setMaxWidth(130);
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
