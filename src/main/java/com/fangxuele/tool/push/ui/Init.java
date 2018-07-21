package com.fangxuele.tool.push.ui;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alee.laf.WebLookAndFeel;
import com.fangxuele.tool.push.bean.UserCase;
import com.fangxuele.tool.push.ui.listener.AboutListener;
import com.fangxuele.tool.push.logic.MsgHisManage;
import com.fangxuele.tool.push.util.Config;
import com.fangxuele.tool.push.util.SystemUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.sun.java.swing.plaf.motif.MotifLookAndFeel;
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import org.apache.commons.lang3.StringUtils;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * 初始化类
 * Created by rememberber(https://github.com/rememberber) on 2017/6/15.
 */
public class Init {

    private static final Log logger = LogFactory.get();

    // 配置文件管理器对象
    public static Config configer = Config.getInstance();

    // 消息管理
    public static MsgHisManage msgHisManager = MsgHisManage.getInstance();

    /**
     * 设置全局字体
     */
    public static void initGlobalFont() {

        // 低分辨率屏幕字号初始化
        String lowDpiKey = "lowDpiInit";
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); //得到屏幕的尺寸
        if (screenSize.width <= 1366 && StringUtils.isEmpty(configer.getProps(lowDpiKey))) {
            configer.setFontSize(15);
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
        for (Enumeration keys = UIManager.getDefaults().keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource)
                UIManager.put(key, fontRes);
        }
    }

    /**
     * 其他初始化
     */
    public static void initOthers() {
        // 设置滚动条速度
        MainWindow.mainWindow.getSettingScrollPane().getVerticalScrollBar().setUnitIncrement(15);
        MainWindow.mainWindow.getSettingScrollPane().getVerticalScrollBar().setDoubleBuffered(true);

        MainWindow.mainWindow.getUserCaseScrollPane().getVerticalScrollBar().setUnitIncrement(15);
        MainWindow.mainWindow.getUserCaseScrollPane().getVerticalScrollBar().setDoubleBuffered(true);

        // 设置版本
        MainWindow.mainWindow.getVersionLabel().setText(ConstantsUI.APP_VERSION);
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
                case "Windows":
                    UIManager.setLookAndFeel(WindowsLookAndFeel.class.getName());
                    break;
                case "Nimbus":
                    UIManager.setLookAndFeel(NimbusLookAndFeel.class.getName());
                    break;
                case "Metal":
                    UIManager.setLookAndFeel(MetalLookAndFeel.class.getName());
                    break;
                case "Motif":
                    UIManager.setLookAndFeel(MotifLookAndFeel.class.getName());
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
    public static void initHelpTab() {

        try {
            MainWindow.mainWindow.getHelpTextPane().setEditable(false);
            HTMLEditorKit kit = new HTMLEditorKit();
            MainWindow.mainWindow.getHelpTextPane().setEditorKit(kit);
            StyleSheet styleSheet = kit.getStyleSheet();
            styleSheet.addRule("h2{color:#FBC87A;}");
            MainWindow.mainWindow.getHelpTextPane().setContentType("text/html; charset=utf-8");
            MainWindow.mainWindow.getHelpTextPane().setPage(MainWindow.class.getResource("/page/help.html"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 初始化他们都在用tab
     */
    public static void initUserCaseTab() {
        // 从github获取用户案例相关信息
        String userCaseInfoContent = HttpUtil.get(ConstantsUI.USER_CASE_URL);
        if (StringUtils.isNotEmpty(userCaseInfoContent)) {
            List<UserCase> userCaseInfoList = JSONUtil.toList(JSONUtil.parseArray(userCaseInfoContent), UserCase.class);

            JPanel userCaseListPanel = MainWindow.mainWindow.getUserCaseListPanel();
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
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    logger.error(e);
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
        MainWindow.mainWindow.setMsgTypeComboBox("");
        MainWindow.mainWindow.setMsgTemplateIdTextField("");
        MainWindow.mainWindow.setMsgTemplateUrlTextField("");
        MainWindow.mainWindow.setMsgKefuMsgTypeComboBox("");
        MainWindow.mainWindow.setMsgKefuMsgTitleTextField("");
        MainWindow.mainWindow.setMsgKefuPicUrlTextField("");
        MainWindow.mainWindow.setMsgKefuDescTextField("");
        MainWindow.mainWindow.setMsgKefuUrlTextField("");
        MainWindow.mainWindow.setMsgTemplateMiniAppidTextField("");
        MainWindow.mainWindow.setMsgTemplateMiniPagePathTextField("");
        MainWindow.mainWindow.setMsgTemplateKeyWordTextField("");
        MainWindow.mainWindow.setMsgYunpianMsgContentTextField("");

        String msgName;
        if (StringUtils.isEmpty(selectedMsgName)) {
            msgName = configer.getMsgName();
        } else {
            msgName = selectedMsgName;
        }

        MainWindow.mainWindow.setMsgNameField(msgName);
        MainWindow.mainWindow.setPreviewUserField(configer.getPreviewUser());

        Map<String, String[]> msgMap = msgHisManager.readMsgHis();

        if (msgMap != null && msgMap.size() != 0) {
            if (msgMap.containsKey(msgName)) {
                String[] msgDataArray = msgMap.get(msgName);
                String msgType = msgDataArray[1];
                MainWindow.mainWindow.setMsgTypeComboBox(msgType);
                MainWindow.mainWindow.setMsgTemplateIdTextField(msgDataArray[2]);
                MainWindow.mainWindow.setMsgTemplateUrlTextField(msgDataArray[3]);
                String kefuMsgType = msgDataArray[4];
                MainWindow.mainWindow.setMsgKefuMsgTypeComboBox(kefuMsgType);
                MainWindow.mainWindow.setMsgKefuMsgTitleTextField(msgDataArray[5]);
                MainWindow.mainWindow.setMsgKefuPicUrlTextField(msgDataArray[6]);
                MainWindow.mainWindow.setMsgKefuDescTextField(msgDataArray[7]);
                MainWindow.mainWindow.setMsgKefuUrlTextField(msgDataArray[8]);
                if (msgDataArray.length > 12) {
                    MainWindow.mainWindow.setMsgYunpianMsgContentTextField(msgDataArray[12]);
                }
                if (msgDataArray.length > 11) {
                    MainWindow.mainWindow.setMsgTemplateKeyWordTextField(msgDataArray[11]);
                }
                if (msgDataArray.length > 9) {
                    MainWindow.mainWindow.setMsgTemplateMiniAppidTextField(msgDataArray[9]);
                    MainWindow.mainWindow.setMsgTemplateMiniPagePathTextField(msgDataArray[10]);
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
                        setCellRenderer(new ButtonColumn(MainWindow.mainWindow.getTemplateMsgDataTable(), headerNames.length - 1));
                MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().
                        getColumn(headerNames.length - 1).
                        setCellEditor(new ButtonColumn(MainWindow.mainWindow.getTemplateMsgDataTable(), headerNames.length - 1));

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
            switchMsgType(MainWindow.mainWindow.getMsgTypeComboBox().getSelectedItem().toString());
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
            case "模板消息":
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
            case "模板消息-小程序":
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
            case "客服消息":
                MainWindow.mainWindow.getKefuMsgPanel().setVisible(true);
                MainWindow.mainWindow.getPreviewMemberLabel().setText("预览消息用户openid（多个以半角分号分隔）");
                break;
            case "客服消息优先":
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
            case "阿里云短信":
            case "腾讯云短信":
            case "阿里大于模板短信":
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
            case "云片网短信":
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
     * @param msgType
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
        MainWindow.mainWindow.setImportFromSqlTextArea(configer.getMemberSql());
        MainWindow.mainWindow.setMemberFilePathField(configer.getMemberFilePath());

        MainWindow.mainWindow.getMemberHisComboBox().removeAllItems();

        File pushHisDir = new File(SystemUtil.configHome + "data" + File.separator + "push_his");
        if (!pushHisDir.exists()) {
            pushHisDir.mkdirs();
        }

        File[] files = pushHisDir.listFiles();
        if (files.length > 0) {
            for (File file : files) {
                MainWindow.mainWindow.getMemberHisComboBox().addItem(file.getName());
            }
        }
    }

    /**
     * 初始化推送tab
     */
    public static void initPushTab() {
        MainWindow.mainWindow.setPushMsgName(configer.getMsgName());
        MainWindow.mainWindow.setPushPageSizeTextField(configer.getRecordPerPage());
        MainWindow.mainWindow.setPushPagePerThreadTextField(configer.getPagePerThread());
        MainWindow.mainWindow.setDryRunCheckBox(configer.isDryRun());
    }

    /**
     * 初始化计划任务tab
     */
    public static void initScheduleTab() {
        // 开始
        MainWindow.mainWindow.setRunAtThisTimeRadioButton(configer.isRadioStartAt());
        MainWindow.mainWindow.setStartAtThisTimeTextField(configer.getTextStartAt());

        //每天
        MainWindow.mainWindow.setRunPerDayRadioButton(configer.isRadioPerDay());
        MainWindow.mainWindow.setStartPerDayTextField(configer.getTextPerDay());

        // 每周
        MainWindow.mainWindow.setRunPerWeekRadioButton(configer.isRadioPerWeek());
        MainWindow.mainWindow.setSchedulePerWeekComboBox(configer.getTextPerWeekWeek());
        MainWindow.mainWindow.setStartPerWeekTextField(configer.getTextPerWeekTime());
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
        if (files.length > 0) {
            for (File file : files) {
                data = new Object[2];
                data[0] = false;
                data[1] = file.getName();
                model.addRow(data);
            }
        }
        MainWindow.mainWindow.getPushHisLeftTable().getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        MainWindow.mainWindow.getPushHisLeftTable().getColumnModel().getColumn(0).setCellRenderer(new MyCheckBoxRenderer());
        // 设置列宽
        MainWindow.mainWindow.getPushHisLeftTable().getColumnModel().getColumn(0).setPreferredWidth(30);
        MainWindow.mainWindow.getPushHisLeftTable().getColumnModel().getColumn(0).setMaxWidth(30);
    }

    /**
     * 初始化设置tab
     */
    public static void initSettingTab() {
        // 常规
        MainWindow.mainWindow.setAutoCheckUpdateCheckBox(configer.isAutoCheckUpdate());

        // 微信公众号
        MainWindow.mainWindow.setWechatAppIdTextField(configer.getWechatAppId());
        MainWindow.mainWindow.setWechatAppSecretPasswordField(configer.getWechatAppSecret());
        MainWindow.mainWindow.setWechatTokenPasswordField(configer.getWechatToken());
        MainWindow.mainWindow.setWechatAesKeyPasswordField(configer.getWechatAesKey());

        // 微信小程序
        MainWindow.mainWindow.setMiniAppAppIdTextField(configer.getMiniAppAppId());
        MainWindow.mainWindow.setMiniAppAppSecretPasswordField(configer.getMiniAppAppSecret());
        MainWindow.mainWindow.setMiniAppTokenPasswordField(configer.getMiniAppToken());
        MainWindow.mainWindow.setMiniAppAesKeyPasswordField(configer.getMiniAppAesKey());

        // 阿里云短信
        MainWindow.mainWindow.setAliyunAccessKeyIdTextField(configer.getAliyunAccessKeyId());
        MainWindow.mainWindow.setAliyunAccessKeySecretTextField(configer.getAliyunAccessKeySecret());
        MainWindow.mainWindow.setAliyunSignTextField(configer.getAliyunSign());

        // 阿里大于
        MainWindow.mainWindow.setAliServerUrlTextField(configer.getAliServerUrl());
        MainWindow.mainWindow.setAliAppKeyPasswordField(configer.getAliAppKey());
        MainWindow.mainWindow.setAliAppSecretPasswordField(configer.getAliAppSecret());
        MainWindow.mainWindow.setAliSignTextField(configer.getAliSign());

        // 腾讯云短信
        MainWindow.mainWindow.setTxyunAppIdTextField(configer.getTxyunAppId());
        MainWindow.mainWindow.setTxyunAppKeyTextField(configer.getTxyunAppKey());
        MainWindow.mainWindow.setTxyunSignTextField(configer.getTxyunSign());

        // 云片网短信
        MainWindow.mainWindow.setYunpianApiKeyTextField(configer.getYunpianApiKey());

        // MySQL
        MainWindow.mainWindow.setMysqlUrlTextField(configer.getMysqlUrl());
        MainWindow.mainWindow.setMysqlDatabaseTextField(configer.getMysqlDatabase());
        MainWindow.mainWindow.setMysqlUserTextField(configer.getMysqlUser());
        MainWindow.mainWindow.setMysqlPasswordField(configer.getMysqlPassword());

        // 外观
        MainWindow.mainWindow.setSettingThemeComboBox(configer.getTheme());
        MainWindow.mainWindow.setSettingFontNameComboBox(configer.getFont());
        MainWindow.mainWindow.setSettingFontSizeComboBox(configer.getFontSize());

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
        MainWindow.mainWindow.getMsgHistable().getColumnModel().getColumn(0).setCellRenderer(new MyCheckBoxRenderer());
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
                setCellRenderer(new ButtonColumn(MainWindow.mainWindow.getTemplateMsgDataTable(), headerNames.length - 1));
        MainWindow.mainWindow.getTemplateMsgDataTable().getColumnModel().
                getColumn(headerNames.length - 1).
                setCellEditor(new ButtonColumn(MainWindow.mainWindow.getTemplateMsgDataTable(), headerNames.length - 1));

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
        new Thread(() -> initUserCaseTab()).start();
        initMsgTab(null);
        initMemberTab();
        initPushTab();
        initScheduleTab();
        // 初始化后置，切换tab时再触发
        // initPushHisTab();
        initSettingTab();

        // 检查新版版
        if (configer.isAutoCheckUpdate()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    AboutListener.checkUpdate(true);
                }
            }).start();
        }
    }

    /**
     * 自定义单元格按钮渲染器
     */
    public static class ButtonColumn extends AbstractCellEditor implements
            TableCellRenderer, TableCellEditor, ActionListener {
        JTable table;
        JButton renderButton;
        JButton editButton;

        public ButtonColumn(JTable table, int column) {
            super();
            this.table = table;
            renderButton = new JButton();
            editButton = new JButton();
            editButton.setFocusPainted(false);
            editButton.addActionListener(this);

            TableColumnModel columnModel = table.getColumnModel();
            columnModel.getColumn(column).setCellRenderer(this);
            columnModel.getColumn(column).setCellEditor(this);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (hasFocus) {
                renderButton.setForeground(table.getForeground());
                renderButton.setBackground(UIManager.getColor("Button.background"));
            } else if (isSelected) {
                renderButton.setForeground(table.getSelectionForeground());
                renderButton.setBackground(table.getSelectionBackground());
            } else {
                renderButton.setForeground(table.getForeground());
                renderButton.setBackground(UIManager.getColor("Button.background"));
            }

            renderButton.setText("移除");
            renderButton.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
            return renderButton;
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            editButton.setText("移除");
            editButton.setIcon(new ImageIcon(getClass().getResource("/icon/remove.png")));
            return editButton;
        }

        public Object getCellEditorValue() {
            return "移除";
        }

        public void actionPerformed(ActionEvent e) {
            int isDelete = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getMessagePanel(), "确认移除？", "确认",
                    JOptionPane.INFORMATION_MESSAGE);
            if (isDelete == JOptionPane.YES_OPTION) {
                fireEditingStopped();
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.removeRow(table.getSelectedRow());
            }
        }
    }

    /**
     * 自定义单元格单选框渲染器
     */
    public static class MyCheckBoxRenderer extends JCheckBox implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Boolean b = (Boolean) value;//这一列必须都是integer类型(0-100)
            setSelected(b);
            return this;
        }
    }

}
