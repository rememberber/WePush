package com.fangxuele.tool.push.util;

import java.io.File;

/**
 * <pre>
 * 系统工具
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/4/20.
 */
public class SystemUtil {
    private static String osName = System.getProperty("os.name");
    private static String osArch = System.getProperty("os.arch");
    public static String configHome = System.getProperty("user.home") + File.separator + ".wepush"
            + File.separator;

    public static boolean isMacOs() {
        return osName.contains("Mac");
    }

    public static boolean isMacM1() {
        return osName.contains("Mac") && "aarch64".equals(osArch);
    }
}