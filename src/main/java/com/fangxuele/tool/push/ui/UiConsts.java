package com.fangxuele.tool.push.ui;

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
    public final static String APP_VERSION = "v_3.0.0_190516";

    /**
     * 主窗口图标
     */
    public final static Image IMAGE_ICON = Toolkit.getDefaultToolkit()
            .getImage(UiConsts.class.getResource("/icon/logo-lg.png"));

    /**
     * 软件版本检查url
     */
    public final static String CHECK_VERSION_URL = "https://raw.githubusercontent.com/rememberber/WePush/master/src/main/resources/version_summary.json";

    /**
     * 用户案例url
     */
    public final static String USER_CASE_URL = "http://download.zhoubochina.com/file/user_case.json";

    /**
     * 用户案例url
     */
    public final static String QR_CODE_URL = "http://download.zhoubochina.com/file/wepush_qrcode.json";

}
