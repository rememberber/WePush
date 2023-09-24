package com.fangxuele.tool.push.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;

/**
 * <pre>
 * UI相关的常量
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/4/20.
 */
public class UiConsts {

    /**
     * 软件名称,版本
     */
    public final static String APP_NAME = "WePush";
    public final static String APP_VERSION = "v_5.0.3";

    public static final int TABLE_ROW_HEIGHT = 30;

    /**
     * Logo-1024*1024
     */
    public static final Image IMAGE_LOGO_1024 = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/logo-1024.png"));

    /**
     * Logo-512*512
     */
    public static final Image IMAGE_LOGO_512 = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/logo-512.png"));

    /**
     * Logo-256*256
     */
    public static final Image IMAGE_LOGO_256 = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/logo-256.png"));

    /**
     * Logo-128*128
     */
    public static final Image IMAGE_LOGO_128 = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/logo-128.png"));

    /**
     * Logo-64*64
     */
    public static final Image IMAGE_LOGO_64 = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/logo-64.png"));

    /**
     * Logo-48*48
     */
    public static final Image IMAGE_LOGO_48 = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/logo-48.png"));

    /**
     * Logo-32*32
     */
    public static final Image IMAGE_LOGO_32 = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/logo-32.png"));

    /**
     * Logo-24*24
     */
    public static final Image IMAGE_LOGO_24 = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/logo-24.png"));

    /**
     * Logo-16*16
     */
    public static final Image IMAGE_LOGO_16 = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/logo-16.png"));

    /**
     * 帮助图标
     */
    public final static Icon HELP_ICON = new FlatSVGIcon("icon/help.svg");

    /**
     * 帮助图标-focused
     */
    public final static Icon HELP_FOCUSED_ICON = new FlatSVGIcon("icon/help-filling.svg");

    /**
     * 软件版本检查url
     */
    public final static String CHECK_VERSION_URL = "https://gitee.com/zhoubochina/WePush/raw/5.x/src/main/resources/version_summary.json";

    /**
     * 用户案例url
     */
    public final static String USER_CASE_URL = "https://gitee.com/zhoubochina/WePush/raw/5.x/assets/files/user_case.json";

    /**
     * 二维码url
     */
    public final static String QR_CODE_URL = "https://gitee.com/zhoubochina/WePush/raw/5.x/assets/files/wx-zanshang.jpg";

    /**
     * 介绍二维码URL
     */
    public final static String INTRODUCE_QRCODE_URL = "https://gitee.com/zhoubochina/WePush/raw/5.x/assets/files/introduce-wepush-qrcode.png";

    public static final String DOWNLOAD_LINK_INFO_URL = "https://gitee.com/zhoubochina/WePush/raw/5.x/assets/files/download_links.json";
}
