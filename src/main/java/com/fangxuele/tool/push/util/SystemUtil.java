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
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String OS_ARCH = System.getProperty("os.arch");
    private static final String VM_VENDOR = System.getProperty("java.vm.vendor");
    private static final String USER_HOME = System.getProperty("user.home");
    public static final String CONFIG_HOME = USER_HOME + File.separator + ".wepush" + File.separator;
    /**
     * 日志文件路径
     */
    public final static String LOG_DIR = USER_HOME + File.separator + ".wepush" + File.separator + "logs" + File.separator;

    public static boolean isMacOs() {
        return OS_NAME.contains("Mac");
    }

    public static boolean isLinuxOs() {
        return OS_NAME.contains("Linux");
    }

    public static boolean isMacM1() {
        return OS_NAME.contains("Mac") && "aarch64".equals(OS_ARCH);
    }

    public static boolean isJBR() {
        return VM_VENDOR.contains("JetBrains");
    }
}