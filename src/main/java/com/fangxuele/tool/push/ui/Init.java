package com.fangxuele.tool.push.ui;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alee.laf.WebLookAndFeel;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TPushHistoryMapper;
import com.fangxuele.tool.push.logic.MessageTypeConsts;
import com.fangxuele.tool.push.logic.MsgHisManage;
import com.fangxuele.tool.push.ui.component.TableInCellButtonColumn;
import com.fangxuele.tool.push.ui.form.AboutForm;
import com.fangxuele.tool.push.ui.form.HelpForm;
import com.fangxuele.tool.push.ui.form.MemberForm;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.PushForm;
import com.fangxuele.tool.push.ui.form.ScheduleForm;
import com.fangxuele.tool.push.ui.form.SettingForm;
import com.fangxuele.tool.push.ui.form.UserCaseForm;
import com.fangxuele.tool.push.ui.listener.AboutListener;
import com.fangxuele.tool.push.util.ConfigUtil;
import com.fangxuele.tool.push.util.SystemUtil;
import org.apache.commons.lang3.StringUtils;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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
     * 消息管理
     */
    public static MsgHisManage msgHisManager = MsgHisManage.getInstance();

    private TPushHistoryMapper pushHistoryMapper = App.sqlSession.getMapper(TPushHistoryMapper.class);

    /**
     * 配置文件管理器对象
     */
    public static ConfigUtil config = ConfigUtil.getInstance();

    /**
     * 设置全局字体
     */
    public static void initGlobalFont() {

        // 低分辨率屏幕字号初始化
        String lowDpiKey = "lowDpiInit";
        // 得到屏幕的尺寸
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (screenSize.width <= 1366 && StringUtils.isEmpty(config.getProps(lowDpiKey))) {
            config.setFontSize(13);
            config.setProps(lowDpiKey, "true");
            config.save();
        }

        // Mac高分辨率屏幕字号初始化
        String highDpiKey = "highDpiInit";
        if (SystemUtil.isMacOs() && StringUtils.isEmpty(config.getProps(highDpiKey))) {
            config.setFontSize(15);
            config.setProps(highDpiKey, "true");
            config.save();
        }

        Font fnt = new Font(config.getFont(), Font.PLAIN, config.getFontSize());
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
            switch (config.getTheme()) {
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
            msgName = config.getMsgName();
        } else {
            msgName = selectedMsgName;
        }

        MessageEditForm.messageEditForm.getMsgNameField().setText(msgName);
        MessageEditForm.messageEditForm.getPreviewUserField().setText(config.getPreviewUser());

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
        HelpForm.init();
        ThreadUtil.execute(UserCaseForm::init);
        initMsgTab(null);
        MemberForm.init();
        PushForm.init();
        ScheduleForm.init();
        SettingForm.init();

        // 检查新版版
        if (config.isAutoCheckUpdate()) {
            ThreadUtil.execute(() -> AboutListener.checkUpdate(true));
        }
        // 更新二维码
        ThreadUtil.execute(AboutListener::initQrCode);
    }

}
