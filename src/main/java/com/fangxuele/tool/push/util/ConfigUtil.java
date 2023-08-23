package com.fangxuele.tool.push.util;

/**
 * <pre>
 * 配置管理
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/14.
 */
public class ConfigUtil extends ConfigBaseUtil {

    private static ConfigUtil configUtil = new ConfigUtil();

    public static ConfigUtil getInstance() {
        return configUtil;
    }

    private ConfigUtil() {
        super();
    }

    public int getMsgType() {
        return setting.getInt("msgType", "msg", 13);
    }

    public void setMsgType(int msgType) {
        setting.put("msg", "msgType", String.valueOf(msgType));
    }

    public String getMsgName() {
        return setting.getStr("msgName", "msg", "");
    }

    public void setMsgName(String msgName) {
        setting.put("msg", "msgName", msgName);
    }

    public String getMemberSql() {
        return setting.getStr("sql", "member", "");
    }

    public void setMemberSql(String memberSql) {
        setting.put("member", "sql", memberSql);
    }

    public boolean isDryRun() {
        return setting.getBool("dryRun", "push", true);
    }

    public void setDryRun(boolean dryRun) {
        setting.put("push", "dryRun", String.valueOf(dryRun));
    }

    public boolean isAutoCheckUpdate() {
        return setting.getBool("autoCheckUpdate", "setting.normal", true);
    }

    public void setAutoCheckUpdate(boolean autoCheckUpdate) {
        setting.put("setting.normal", "autoCheckUpdate", String.valueOf(autoCheckUpdate));
    }

    public boolean isUseTray() {
        return setting.getBool("useTray", "setting.normal", true);
    }

    public void setUseTray(boolean useTray) {
        setting.put("setting.normal", "useTray", String.valueOf(useTray));
    }

    public boolean isCloseToTray() {
        return setting.getBool("closeToTray", "setting.normal", true);
    }

    public void setCloseToTray(boolean closeToTray) {
        setting.put("setting.normal", "closeToTray", String.valueOf(closeToTray));
    }

    public boolean isDefaultMaxWindow() {
        return setting.getBool("defaultMaxWindow", "setting.normal", true);
    }

    public void setDefaultMaxWindow(boolean defaultMaxWindow) {
        setting.put("setting.normal", "defaultMaxWindow", String.valueOf(defaultMaxWindow));
    }

    public boolean isUnifiedBackground() {
        return setting.getBool("unifiedBackground", "setting.normal", true);
    }

    public void setUnifiedBackground(boolean unifiedBackground) {
        setting.put("setting.normal", "unifiedBackground", String.valueOf(unifiedBackground));
    }

    public Integer getMaxThreads() {
        return setting.getInt("maxThreads", "setting.normal", 100);
    }

    public void setMaxThreads(Integer maxThreads) {
        setting.put("setting.normal", "maxThreads", String.valueOf(maxThreads));
    }

    public long getPushTotal() {
        return setting.getLong("pushTotal", "setting.normal", 0L);
    }

    public void setPushTotal(long pushTotal) {
        setting.put("setting.normal", "pushTotal", String.valueOf(pushTotal));
    }

    public String getBeforeVersion() {
        return setting.getStr("beforeVersion", "setting.normal", "v_0.0.0");
    }

    public void setBeforeVersion(String beforeVersion) {
        setting.put("setting.normal", "beforeVersion", beforeVersion);
    }

    public String getMailHost() {
        return setting.getStr("mailHost", "setting.mail", "smtp.163.com");
    }

    public void setMailHost(String mailHost) {
        setting.put("setting.mail", "mailHost", mailHost);
    }

    public String getMailPort() {
        return setting.getStr("mailPort", "setting.mail", "25");
    }

    public void setMailPort(String mailPort) {
        setting.put("setting.mail", "mailPort", mailPort);
    }

    public String getMailFrom() {
        return setting.getStr("mailFrom", "setting.mail", "");
    }

    public void setMailFrom(String mailFrom) {
        setting.put("setting.mail", "mailFrom", mailFrom);
    }

    public String getMailUser() {
        return setting.getStr("mailUser", "setting.mail", "");
    }

    public void setMailUser(String mailUser) {
        setting.put("setting.mail", "mailUser", mailUser);
    }

    public String getMailPassword() {
        return setting.getStr("mailPassword", "setting.mail", "");
    }

    public void setMailPassword(String mailPassword) {
        setting.put("setting.mail", "mailPassword", mailPassword);
    }

    public boolean isMailUseStartTLS() {
        return setting.getBool("mailUseStartTLS", "setting.mail", false);
    }

    public void setMailUseStartTLS(boolean mailUseStartTLS) {
        setting.put("setting.mail", "mailUseStartTLS", String.valueOf(mailUseStartTLS));
    }

    public boolean isMailUseSSL() {
        return setting.getBool("mailUseSSL", "setting.mail", false);
    }

    public void setMailUseSSL(boolean mailUseSSL) {
        setting.put("setting.mail", "mailUseSSL", String.valueOf(mailUseSSL));
    }

    public String getMysqlUrl() {
        return setting.getStr("url", "setting.mysql", "");
    }

    public void setMysqlUrl(String mysqlUrl) {
        setting.put("setting.mysql", "url", mysqlUrl);
    }

    public String getMysqlUser() {
        return setting.getStr("user", "setting.mysql", "");
    }

    public void setMysqlUser(String mysqlUser) {
        setting.put("setting.mysql", "user", mysqlUser);
    }

    public String getMysqlPassword() {
        return setting.getStr("password", "setting.mysql", "");
    }

    public void setMysqlPassword(String mysqlPassword) {
        setting.put("setting.mysql", "password", mysqlPassword);
    }

    public String getTheme() {
        return setting.getStr("theme", "setting.appearance", "Flat macOS Dark");
    }

    public void setTheme(String theme) {
        setting.put("setting.appearance", "theme", theme);
    }

    public String getFont() {
        if (SystemUtil.isLinuxOs()) {
            return setting.getStr("font", "setting.appearance", "Noto Sans CJK HK");
        } else if (SystemUtil.isMacOs()) {
            return setting.getStr("font", "setting.appearance", "PingFang SC");
        } else {
            return setting.getStr("font", "setting.appearance", "微软雅黑");
        }
    }

    public void setFont(String font) {
        setting.put("setting.appearance", "font", font);
    }

    public int getFontSize() {
        return setting.getInt("fontSize", "setting.appearance", 12);
    }

    public void setFontSize(int fontSize) {
        setting.put("setting.appearance", "fontSize", String.valueOf(fontSize));
    }

}
