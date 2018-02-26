package com.fangxuele.tool.wechat.push.util;

/**
 * 系统工具
 */
public class SystemUtil {
    public static String osName = System.getProperty("os.name");

    public static boolean isMacOs() {
        if (osName.contains("Mac")) {
            return true;
        } else {
            return false;
        }
    }
}
