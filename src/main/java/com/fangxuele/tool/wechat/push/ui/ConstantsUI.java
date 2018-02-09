package com.fangxuele.tool.wechat.push.ui;

import java.awt.Image;
import java.awt.Toolkit;

/**
 * UI相关的常量
 *
 * @author rememberber(https://github.com/rememberber)
 */
public class ConstantsUI {

    /**
     * 软件名称,版本
     */
    public final static String APP_NAME = "WePush";
    public final static String APP_VERSION = "v_1.4.0_180209";

    /**
     * 主窗口图标
     */
    public final static Image IMAGE_ICON = Toolkit.getDefaultToolkit()
            .getImage(MainWindow.class.getResource("/icon/logo-md.png"));

}
