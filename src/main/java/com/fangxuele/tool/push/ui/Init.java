package com.fangxuele.tool.push.ui;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alee.laf.WebLookAndFeel;
import com.fangxuele.tool.push.ui.form.AboutForm;
import com.fangxuele.tool.push.ui.form.HelpForm;
import com.fangxuele.tool.push.ui.form.MemberForm;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.ui.form.MessageManageForm;
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
     * 初始化所有tab
     */
    public static void initAllTab() {
        HelpForm.init();
        ThreadUtil.execute(UserCaseForm::init);
        MessageEditForm.init(null);
        MessageManageForm.init();
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
