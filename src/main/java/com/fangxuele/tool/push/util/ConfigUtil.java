package com.fangxuele.tool.push.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.setting.Setting;

import java.io.File;

/**
 * <pre>
 * 配置管理
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/14.
 */
public class ConfigUtil {
    /**
     * 设置文件路径
     */
    private String settingFilePath = SystemUtil.configHome + "config" + File.separator + "config.setting";

    private Setting setting;

    private static ConfigUtil configUtil = new ConfigUtil();

    private int msgType;

    private String msgName;

    private String previewUser;

    private String memberSql;

    private String memberFilePath;

    private int maxThreadPool;

    private int threadCount;

    private boolean dryRun;

    private boolean radioPerDay;

    private String textPerDay;

    private boolean radioPerWeek;

    private String textPerWeekWeek;

    private String textPerWeekTime;

    private boolean needReimport;

    private String reimportWay;

    private boolean sendPushResult;

    private String mailResultTos;

    private boolean autoCheckUpdate;

    private long pushTotal;

    private String beforeVersion;

    private String wechatMpName;

    private String wechatAppId;

    private String wechatAppSecret;

    private String wechatToken;

    private String wechatAesKey;

    private boolean mpUseProxy;

    private String mpProxyHost;

    private String mpProxyPort;

    private String mpProxyUserName;

    private String mpProxyPassword;

    private String miniAppName;

    private String miniAppAppId;

    private String miniAppAppSecret;

    private String miniAppToken;

    private String miniAppAesKey;

    private boolean maUseProxy;

    private String maProxyHost;

    private String maProxyPort;

    private String maProxyUserName;

    private String maProxyPassword;

    private String aliyunAccessKeyId;

    private String aliyunAccessKeySecret;

    private String aliyunSign;

    private String aliServerUrl;

    private String aliAppKey;

    private String aliAppSecret;

    private String aliSign;

    private String txyunAppId;

    private String txyunAppKey;

    private String txyunSign;

    private String yunpianApiKey;

    private String mailHost;

    private String mailPort;

    private String mailFrom;

    private String mailUser;

    private String mailPassword;

    private boolean mailUseStartTLS;

    private boolean mailUseSSL;

    private String mysqlUrl;

    private String mysqlDatabase;

    private String mysqlUser;

    private String mysqlPassword;

    private String theme;

    private String font;

    private int fontSize;

    public static ConfigUtil getInstance() {
        return configUtil;
    }

    private ConfigUtil() {
        setting = new Setting(FileUtil.touch(settingFilePath), CharsetUtil.CHARSET_UTF_8, false);
    }

    public void setProps(String key, String value) {
        setting.put(key, value);
    }

    public String getProps(String key) {
        return setting.get(key);
    }

    /**
     * 存盘
     */
    public void save() {
        setting.store(settingFilePath);
    }

    public int getMsgType() {
        return setting.getInt("msgType", "msg", 1);
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

    public String getPreviewUser() {
        return setting.getStr("previewUser", "msg", "");
    }

    public void setPreviewUser(String previewUser) {
        setting.put("msg", "previewUser", previewUser);
    }

    public String getMemberSql() {
        return setting.getStr("sql", "member", "");
    }

    public void setMemberSql(String memberSql) {
        setting.put("member", "sql", memberSql);
    }

    public String getMemberFilePath() {
        return setting.getStr("filePath", "member", "");
    }

    public void setMemberFilePath(String memberFilePath) {
        setting.put("member", "filePath", memberFilePath);
    }

    public int getMaxThreadPool() {
        return setting.getInt("maxThreadPool", "push", 100);
    }

    public void setMaxThreadPool(int maxThreadPool) {
        setting.put("push", "maxThreadPool", String.valueOf(maxThreadPool));
    }

    public int getThreadCount() {
        return setting.getInt("threadCount", "push", 60);
    }

    public void setThreadCount(int threadCount) {
        setting.put("push", "threadCount", String.valueOf(threadCount));
    }

    public boolean isDryRun() {
        return setting.getBool("dryRun", "push", true);
    }

    public void setDryRun(boolean dryRun) {
        setting.put("push", "dryRun", String.valueOf(dryRun));
    }

    public boolean isRadioStartAt() {
        return setting.getBool("radioStartAt", "schedule", false);
    }

    public void setRadioStartAt(boolean radioStartAt) {
        setting.put("schedule", "radioStartAt", String.valueOf(radioStartAt));
    }

    public String getTextStartAt() {
        return setting.getStr("textStartAt", "schedule", "");
    }

    public void setTextStartAt(String textStartAt) {
        setting.put("schedule", "textStartAt", textStartAt);
    }

    public boolean isRadioPerDay() {
        return setting.getBool("radioPerDay", "schedule", false);
    }

    public void setRadioPerDay(boolean radioPerDay) {
        setting.put("schedule", "radioPerDay", String.valueOf(radioPerDay));
    }

    public String getTextPerDay() {
        return setting.getStr("textPerDay", "schedule", "");
    }

    public void setTextPerDay(String textPerDay) {
        setting.put("schedule", "textPerDay", textPerDay);
    }

    public boolean isRadioPerWeek() {
        return setting.getBool("radioPerWeek", "schedule", false);
    }

    public void setRadioPerWeek(boolean radioPerWeek) {
        setting.put("schedule", "radioPerWeek", String.valueOf(radioPerWeek));
    }

    public String getTextPerWeekWeek() {
        return setting.getStr("textPerWeek.week", "schedule", "一");
    }

    public void setTextPerWeekWeek(String textPerWeekWeek) {
        setting.put("schedule", "textPerWeek.week", textPerWeekWeek);
    }

    public String getTextPerWeekTime() {
        return setting.getStr("textPerWeek.time", "schedule", "");
    }

    public void setTextPerWeekTime(String textPerWeekTime) {
        setting.put("schedule", "textPerWeek.time", textPerWeekTime);
    }

    public boolean isNeedReimport() {
        return setting.getBool("reimportCheckBox", "schedule", false);
    }

    public void setNeedReimport(boolean needReimport) {
        setting.put("schedule", "reimportCheckBox", String.valueOf(needReimport));
    }

    public String getReimportWay() {
        return setting.getStr("reimportComboBox", "schedule", "");
    }

    public void setReimportWay(String reimportWay) {
        setting.put("schedule", "reimportComboBox", reimportWay);
    }

    public boolean isSendPushResult() {
        return setting.getBool("sendPushResult", "schedule", false);
    }

    public void setSendPushResult(boolean sendPushResult) {
        setting.put("schedule", "sendPushResult", String.valueOf(sendPushResult));
    }

    public String getMailResultTos() {
        return setting.getStr("mailResultTos", "schedule", "");
    }

    public void setMailResultTos(String mailResultTos) {
        setting.put("schedule", "mailResultTos", mailResultTos);
    }

    public boolean isAutoCheckUpdate() {
        return setting.getBool("autoCheckUpdate", "setting.normal", true);
    }

    public void setAutoCheckUpdate(boolean autoCheckUpdate) {
        setting.put("setting.normal", "autoCheckUpdate", String.valueOf(autoCheckUpdate));
    }

    public long getPushTotal() {
        return setting.getLong("pushTotal", "setting.normal", 0L);
    }

    public void setPushTotal(long pushTotal) {
        setting.put("setting.normal", "pushTotal", String.valueOf(pushTotal));
    }

    public String getBeforeVersion() {
        return setting.getStr("beforeVersion", "setting.normal", "v_3.0.0_190516");
    }

    public void setBeforeVersion(String beforeVersion) {
        setting.put("setting.normal", "beforeVersion", beforeVersion);
    }

    public String getWechatMpName() {
        return setting.getStr("mpName", "setting.wechat", "默认公众号");
    }

    public void setWechatMpName(String wechatMpName) {
        setting.put("setting.wechat", "mpName", wechatMpName);
    }

    public String getWechatAppId() {
        return setting.getStr("appId", "setting.wechat", "");
    }

    public void setWechatAppId(String wechatAppId) {
        setting.put("setting.wechat", "appId", wechatAppId);
    }

    public String getWechatAppSecret() {
        return setting.getStr("AppSecret", "setting.wechat", "");
    }

    public void setWechatAppSecret(String wechatAppSecret) {
        setting.put("setting.wechat", "AppSecret", wechatAppSecret);
    }

    public String getWechatToken() {
        return setting.getStr("token", "setting.wechat", "");
    }

    public void setWechatToken(String wechatToken) {
        setting.put("setting.wechat", "token", wechatToken);
    }

    public String getWechatAesKey() {
        return setting.getStr("aesKey", "setting.wechat", "");
    }

    public void setWechatAesKey(String wechatAesKey) {
        setting.put("setting.wechat", "aesKey", wechatAesKey);
    }

    public boolean isMpUseProxy() {
        return setting.getBool("mpUseProxy", "setting.wechat", false);
    }

    public void setMpUseProxy(boolean mpUseProxy) {
        setting.put("setting.wechat", "mpUseProxy", String.valueOf(mpUseProxy));
    }

    public String getMpProxyHost() {
        return setting.getStr("mpProxyHost", "setting.wechat", "");
    }

    public void setMpProxyHost(String mpProxyHost) {
        setting.put("setting.wechat", "mpProxyHost", mpProxyHost);
    }

    public String getMpProxyPort() {
        return setting.getStr("mpProxyPort", "setting.wechat", "");
    }

    public void setMpProxyPort(String mpProxyPort) {
        setting.put("setting.wechat", "mpProxyPort", mpProxyPort);
    }

    public String getMpProxyUserName() {
        return setting.getStr("mpProxyUserName", "setting.wechat", "");
    }

    public void setMpProxyUserName(String mpProxyUserName) {
        setting.put("setting.wechat", "mpProxyUserName", mpProxyUserName);
    }

    public String getMpProxyPassword() {
        return setting.getStr("mpProxyPassword", "setting.wechat", "");
    }

    public void setMpProxyPassword(String mpProxyPassword) {
        setting.put("setting.wechat", "mpProxyPassword", mpProxyPassword);
    }

    public String getAliServerUrl() {
        return setting.getStr("serverUrl", "setting.ali", "");
    }

    public void setAliServerUrl(String aliServerUrl) {
        setting.put("setting.ali", "serverUrl", aliServerUrl);
    }

    public String getAliAppKey() {
        return setting.getStr("appKey", "setting.ali", "");
    }

    public void setAliAppKey(String aliAppKey) {
        setting.put("setting.ali", "appKey", aliAppKey);
    }

    public String getAliAppSecret() {
        return setting.getStr("appSecret", "setting.ali", "");
    }

    public void setAliAppSecret(String aliAppSecret) {
        setting.put("setting.ali", "appSecret", aliAppSecret);
    }

    public String getAliSign() {
        return setting.getStr("sign", "setting.ali", "");
    }

    public void setAliSign(String aliSign) {
        setting.put("setting.ali", "sign", aliSign);
    }

    public String getMailHost() {
        return setting.getStr("mailHost", "setting.mail", "");
    }

    public void setMailHost(String mailHost) {
        setting.put("setting.mail", "mailHost", mailHost);
    }

    public String getMailPort() {
        return setting.getStr("mailPort", "setting.mail", "");
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

    public String getMysqlDatabase() {
        return setting.getStr("database", "setting.mysql", "");
    }

    public void setMysqlDatabase(String mysqlDatabase) {
        setting.put("setting.mysql", "database", mysqlDatabase);
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
        return setting.getStr("theme", "setting.appearance", "Darcula(推荐)");
    }

    public void setTheme(String theme) {
        setting.put("setting.appearance", "theme", theme);
    }

    public String getFont() {
        return setting.getStr("font", "setting.appearance", "Microsoft YaHei UI");
    }

    public void setFont(String font) {
        setting.put("setting.appearance", "font", font);
    }

    public int getFontSize() {
        return setting.getInt("fontSize", "setting.appearance", 13);
    }

    public void setFontSize(int fontSize) {
        setting.put("setting.appearance", "fontSize", String.valueOf(fontSize));
    }

    public String getAliyunAccessKeyId() {
        return setting.getStr("accessKeyId", "setting.aliyun", "");
    }

    public void setAliyunAccessKeyId(String aliyunAccessKeyId) {
        setting.put("setting.aliyun", "accessKeyId", aliyunAccessKeyId);
    }

    public String getAliyunAccessKeySecret() {
        return setting.getStr("accessKeySecret", "setting.aliyun", "");
    }

    public void setAliyunAccessKeySecret(String aliyunAccessKeySecret) {
        setting.put("setting.aliyun", "accessKeySecret", aliyunAccessKeySecret);
    }

    public String getAliyunSign() {
        return setting.getStr("aliyunSign", "setting.aliyun", "");
    }

    public void setAliyunSign(String aliyunSign) {
        setting.put("setting.aliyun", "aliyunSign", aliyunSign);
    }

    public String getMiniAppName() {
        return setting.getStr("name", "setting.miniApp", "默认小程序");
    }

    public void setMiniAppName(String miniAppName) {
        setting.put("setting.miniApp", "name", miniAppName);
    }

    public String getMiniAppAppId() {
        return setting.getStr("appId", "setting.miniApp", "");
    }

    public void setMiniAppAppId(String miniAppAppId) {
        setting.put("setting.miniApp", "appId", miniAppAppId);
    }

    public String getMiniAppAppSecret() {
        return setting.getStr("AppSecret", "setting.miniApp", "");
    }

    public void setMiniAppAppSecret(String miniAppAppSecret) {
        setting.put("setting.miniApp", "AppSecret", miniAppAppSecret);
    }

    public String getMiniAppToken() {
        return setting.getStr("token", "setting.miniApp", "");
    }

    public void setMiniAppToken(String miniAppToken) {
        setting.put("setting.miniApp", "token", miniAppToken);
    }

    public String getMiniAppAesKey() {
        return setting.getStr("aesKey", "setting.miniApp", "");
    }

    public void setMiniAppAesKey(String miniAppAesKey) {
        setting.put("setting.miniApp", "aesKey", miniAppAesKey);
    }

    public boolean isMaUseProxy() {
        return setting.getBool("maUseProxy", "setting.wechat", false);
    }

    public void setMaUseProxy(boolean maUseProxy) {
        setting.put("setting.wechat", "maUseProxy", String.valueOf(maUseProxy));
    }

    public String getMaProxyHost() {
        return setting.getStr("maProxyHost", "setting.wechat", "");
    }

    public void setMaProxyHost(String maProxyHost) {
        setting.put("setting.wechat", "maProxyHost", maProxyHost);
    }

    public String getMaProxyPort() {
        return setting.getStr("maProxyPort", "setting.wechat", "");
    }

    public void setMaProxyPort(String maProxyPort) {
        setting.put("setting.wechat", "maProxyPort", maProxyPort);
    }

    public String getMaProxyUserName() {
        return setting.getStr("maProxyUserName", "setting.wechat", "");
    }

    public void setMaProxyUserName(String maProxyUserName) {
        setting.put("setting.wechat", "maProxyUserName", maProxyUserName);
    }

    public String getMaProxyPassword() {
        return setting.getStr("maProxyPassword", "setting.wechat", "");
    }

    public void setMaProxyPassword(String maProxyPassword) {
        setting.put("setting.wechat", "maProxyPassword", maProxyPassword);
    }

    public String getTxyunAppId() {
        return setting.getStr("appId", "setting.txyun", "");
    }

    public void setTxyunAppId(String txyunAppId) {
        setting.put("setting.txyun", "appId", txyunAppId);
    }

    public String getTxyunAppKey() {
        return setting.getStr("appKey", "setting.txyun", "");
    }

    public void setTxyunAppKey(String txyunAppKey) {
        setting.put("setting.txyun", "appKey", txyunAppKey);
    }

    public String getTxyunSign() {
        return setting.getStr("txyunSign", "setting.txyun", "");
    }

    public void setTxyunSign(String txyunSign) {
        setting.put("setting.txyun", "txyunSign", txyunSign);
    }

    public String getYunpianApiKey() {
        return setting.getStr("apiKey", "setting.yunpian", "");
    }

    public void setYunpianApiKey(String yunpianApiKey) {
        setting.put("setting.yunpian", "apiKey", yunpianApiKey);
    }
}
