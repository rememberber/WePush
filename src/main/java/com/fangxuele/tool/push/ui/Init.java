package com.fangxuele.tool.push.ui;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.ui.dialog.FontSizeAdjustDialog;
import com.fangxuele.tool.push.ui.form.AboutForm;
import com.fangxuele.tool.push.ui.form.BoostForm;
import com.fangxuele.tool.push.ui.form.HelpForm;
import com.fangxuele.tool.push.ui.form.HttpResultForm;
import com.fangxuele.tool.push.ui.form.MemberForm;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.MessageManageForm;
import com.fangxuele.tool.push.ui.form.MessageTypeForm;
import com.fangxuele.tool.push.ui.form.PushForm;
import com.fangxuele.tool.push.ui.form.PushHisForm;
import com.fangxuele.tool.push.ui.form.ScheduleForm;
import com.fangxuele.tool.push.ui.form.SettingForm;
import com.fangxuele.tool.push.ui.form.UserCaseForm;
import com.fangxuele.tool.push.ui.listener.AboutListener;
import com.fangxuele.tool.push.util.SystemUtil;
import com.fangxuele.tool.push.util.UIUtil;
import com.fangxuele.tool.push.util.UpgradeUtil;
import org.apache.commons.lang3.StringUtils;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Enumeration;

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
     * 字号初始化KEY
     */
    private static final String FONT_SIZE_INIT_PROP = "fontSizeInit";

    /**
     * 设置全局字体
     */
    public static void initGlobalFont() {
        if (StringUtils.isEmpty(App.config.getProps(FONT_SIZE_INIT_PROP))) {
            // 根据DPI调整字号
            // 得到屏幕的分辨率dpi
            // dell 1920*1080/24寸=96
            // 小米air 1920*1080/13.3寸=144
            // 小米air 1366*768/13.3寸=96
            int fontSize = 12;

            // Mac等高分辨率屏幕字号初始化
            if (SystemUtil.isMacOs()) {
                fontSize = 15;
            } else {
                fontSize = (int) (UIUtil.getScreenScale() * fontSize);
            }
            App.config.setFontSize(fontSize);
        }

        Font font = new Font(App.config.getFont(), Font.PLAIN, App.config.getFontSize());
        FontUIResource fontRes = new FontUIResource(font);
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

        MessageEditForm.getInstance().getMsgEditScrollPane().getVerticalScrollBar().setUnitIncrement(15);
        MessageEditForm.getInstance().getMsgEditScrollPane().getVerticalScrollBar().setDoubleBuffered(true);

        SettingForm.getInstance().getSettingScrollPane().getVerticalScrollBar().setUnitIncrement(16);
        SettingForm.getInstance().getSettingScrollPane().getVerticalScrollBar().setDoubleBuffered(true);

        UserCaseForm.getInstance().getUserCaseScrollPane().getVerticalScrollBar().setUnitIncrement(15);
        UserCaseForm.getInstance().getUserCaseScrollPane().getVerticalScrollBar().setDoubleBuffered(true);

        MemberForm.getInstance().getMemberImportScrollPane().getVerticalScrollBar().setUnitIncrement(15);
        MemberForm.getInstance().getMemberImportScrollPane().getVerticalScrollBar().setDoubleBuffered(true);

        MessageTypeForm.getInstance().getMessageTypeScrollPane().getVerticalScrollBar().setUnitIncrement(15);
        MessageTypeForm.getInstance().getMessageTypeScrollPane().getVerticalScrollBar().setDoubleBuffered(true);

        HttpResultForm.getInstance().getHttpResultScrollPane().getVerticalScrollBar().setUnitIncrement(15);
        HttpResultForm.getInstance().getHttpResultScrollPane().getVerticalScrollBar().setDoubleBuffered(true);

        // 设置版本
        AboutForm.getInstance().getVersionLabel().setText(UiConsts.APP_VERSION);
    }

    /**
     * 初始化look and feel
     */
    public static void initTheme() {

        try {
            switch (App.config.getTheme()) {
                case "BeautyEye":
                    BeautyEyeLNFHelper.launchBeautyEyeLNF();
                    UIManager.put("RootPane.setupButtonVisible", false);
                    break;
                case "系统默认":
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
                case "weblaf":
                case "Darcula(推荐)":
                default:
                    UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * 初始化所有tab
     */
    public static void initAllTab() {
        AboutForm.init();
        MessageTypeForm.init();
        HelpForm.init();
        ThreadUtil.execute(UserCaseForm::init);
        MessageEditForm.init(null);
        MessageManageForm.init();
        MemberForm.init();
        PushForm.init();
        BoostForm.init();
        ScheduleForm.init();
        SettingForm.init();
        PushHisForm.init();

        // 检查新版版
        if (App.config.isAutoCheckUpdate()) {
            ThreadUtil.execute(() -> UpgradeUtil.checkUpdate(true));
        }
        // 更新二维码
        ThreadUtil.execute(AboutListener::initQrCode);
    }

    /**
     * 引导用户调整字号
     */
    public static void initFontSize() {
        if (StringUtils.isEmpty(App.config.getProps(FONT_SIZE_INIT_PROP))) {
            FontSizeAdjustDialog fontSizeAdjustDialog = new FontSizeAdjustDialog();
            fontSizeAdjustDialog.pack();
            fontSizeAdjustDialog.setVisible(true);
        }

        App.config.setProps(FONT_SIZE_INIT_PROP, "true");
        App.config.save();
    }
}
