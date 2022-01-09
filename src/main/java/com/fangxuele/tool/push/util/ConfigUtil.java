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

    private int msgType;

    private String msgName;

    private String memberSql;

    private String memberFilePath;

    private int infinityThreadCount;

    private int threadCount;

    private boolean dryRun;

    private boolean radioPerDay;

    private String textPerDay;

    private boolean radioPerWeek;

    private String textPerWeekWeek;

    private String textPerWeekTime;

    private boolean radioCron;

    private String textCron;

    private boolean needReimport;

    private String reimportWay;

    private boolean sendPushResult;

    private String mailResultTos;

    private boolean autoCheckUpdate;

    private boolean useTray;

    private boolean closeToTray;

    private boolean defaultMaxWindow;

    private Integer maxThreads;

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

    private boolean mpUseOutSideAt;

    private boolean mpManualAt;

    private boolean mpApiAt;

    private String mpAt;

    private String mpAtExpiresIn;

    private String mpAtApiUrl;

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

    /**
     * 企业号企业id
     */
    private String wxCpCorpId;

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

    private String hwAppKey;
    private String hwAppSecretPassword;
    private String hwAccessUrl;
    private String hwSignature;
    private String hwSenderCode;

    private String bdSecretAccessKey;
    private String bdAccessKeyId;
    private String bdEndPoint;
    private String bdInvokeId;

    private String upAuthorizationToken;

    private String qiniuAccessKey;
    private String qiniuSecretKey;

    private String yunpianApiKey;

    private boolean httpUseProxy;

    private String httpProxyHost;

    private String httpProxyPort;

    private String httpProxyUserName;

    private String httpProxyPassword;

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

    /**
     * 当前所选的微信账户的id
     */
    private Integer wxAccountId;

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

    public String getMemberFilePath() {
        return setting.getStr("filePath", "member", "");
    }

    public void setMemberFilePath(String memberFilePath) {
        setting.put("member", "filePath", memberFilePath);
    }

    public int getInfinityThreadCount() {
        return setting.getInt("infinityThreadCount", "push", 20);
    }

    public void setInfinityThreadCount(int infinityThreadCount) {
        setting.put("push", "infinityThreadCount", String.valueOf(infinityThreadCount));
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

    public boolean isRadioCron() {
        return setting.getBool("radioCron", "schedule", false);
    }

    public void setRadioCron(boolean radioCron) {
        setting.put("schedule", "radioCron", String.valueOf(radioCron));
    }

    public String getTextCron() {
        return setting.getStr("textCron", "schedule", "");
    }

    public void setTextCron(String textCron) {
        setting.put("schedule", "textCron", textCron);
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

    public boolean isMpUseOutSideAt() {
        return setting.getBool("mpUseOutSideAt", "setting.wechat", false);
    }

    public void setMpUseOutSideAt(boolean mpUseOutSideAt) {
        setting.put("setting.wechat", "mpUseOutSideAt", String.valueOf(mpUseOutSideAt));
    }

    public boolean isMpManualAt() {
        return setting.getBool("mpManualAt", "setting.wechat", false);
    }

    public void setMpManualAt(boolean mpManualAt) {
        setting.put("setting.wechat", "mpManualAt", String.valueOf(mpManualAt));
    }

    public boolean isMpApiAt() {
        return setting.getBool("mpApiAt", "setting.wechat", false);
    }

    public void setMpApiAt(boolean mpApiAt) {
        setting.put("setting.wechat", "mpApiAt", String.valueOf(mpApiAt));
    }

    public String getMpAt() {
        return setting.getStr("mpAt", "setting.wechat", "");
    }

    public void setMpAt(String mpAt) {
        setting.put("setting.wechat", "mpAt", mpAt);
    }

    public String getMpAtExpiresIn() {
        return setting.getStr("mpAtExpiresIn", "setting.wechat", "");
    }

    public void setMpAtExpiresIn(String mpAtExpiresIn) {
        setting.put("setting.wechat", "mpAtExpiresIn", mpAtExpiresIn);
    }

    public String getMpAtApiUrl() {
        return setting.getStr("mpAtApiUrl", "setting.wechat", "");
    }

    public void setMpAtApiUrl(String mpAtApiUrl) {
        setting.put("setting.wechat", "mpAtApiUrl", mpAtApiUrl);
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
        return setting.getStr("theme", "setting.appearance", "Flat Dark");
    }

    public void setTheme(String theme) {
        setting.put("setting.appearance", "theme", theme);
    }

    public String getFont() {
        if (SystemUtil.isLinuxOs()) {
            return setting.getStr("font", "setting.appearance", "Noto Sans CJK HK");
        } else {
            return setting.getStr("font", "setting.appearance", "微软雅黑");
        }
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

    public String getWxCpCorpId() {
        return setting.getStr("wxCpCorpId", "setting.wechat", "");
    }

    public void setWxCpCorpId(String wxCpCorpId) {
        setting.put("setting.wechat", "wxCpCorpId", wxCpCorpId);
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

    public String getHwAppKey() {
        return setting.getStr("hwAppKey", "setting.hwyun", "");
    }

    public void setHwAppKey(String hwAppKey) {
        setting.put("setting.hwyun", "hwAppKey", hwAppKey);
    }

    public String getHwAppSecretPassword() {
        return setting.getStr("hwAppSecretPassword", "setting.hwyun", "");
    }

    public void setHwAppSecretPassword(String hwAppSecretPassword) {
        setting.put("setting.hwyun", "hwAppSecretPassword", hwAppSecretPassword);
    }

    public String getHwAccessUrl() {
        return setting.getStr("hwAccessUrl", "setting.hwyun", "");
    }

    public void setHwAccessUrl(String hwAccessUrl) {
        setting.put("setting.hwyun", "hwAccessUrl", hwAccessUrl);
    }

    public String getHwSignature() {
        return setting.getStr("hwSignature", "setting.hwyun", "");
    }

    public void setHwSignature(String hwSignature) {
        setting.put("setting.hwyun", "hwSignature", hwSignature);
    }

    public String getHwSenderCode() {
        return setting.getStr("hwSenderCode", "setting.hwyun", "");
    }

    public void setHwSenderCode(String hwSenderCode) {
        setting.put("setting.hwyun", "hwSenderCode", hwSenderCode);
    }

    public String getBdSecretAccessKey() {
        return setting.getStr("bdSecretAccessKey", "setting.bdyun", "");
    }

    public void setBdSecretAccessKey(String bdSecretAccessKey) {
        setting.put("setting.bdyun", "bdSecretAccessKey", bdSecretAccessKey);
    }

    public String getBdAccessKeyId() {
        return setting.getStr("bdAccessKeyId", "setting.bdyun", "");
    }

    public void setBdAccessKeyId(String bdAccessKeyId) {
        setting.put("setting.bdyun", "bdAccessKeyId", bdAccessKeyId);
    }

    public String getBdEndPoint() {
        return setting.getStr("bdEndPoint", "setting.bdyun", "");
    }

    public void setBdEndPoint(String bdEndPoint) {
        setting.put("setting.bdyun", "bdEndPoint", bdEndPoint);
    }

    public String getBdInvokeId() {
        return setting.getStr("bdInvokeId", "setting.bdyun", "");
    }

    public void setBdInvokeId(String bdInvokeId) {
        setting.put("setting.bdyun", "bdInvokeId", bdInvokeId);
    }

    public String getUpAuthorizationToken() {
        return setting.getStr("upAuthorizationToken", "setting.upyun", "");
    }

    public void setUpAuthorizationToken(String upAuthorizationToken) {
        setting.put("setting.upyun", "upAuthorizationToken", upAuthorizationToken);
    }

    public String getQiniuAccessKey() {
        return setting.getStr("qiniuAccessKey", "setting.qiniu", "");
    }

    public void setQiniuAccessKey(String qiniuAccessKey) {
        setting.put("setting.qiniu", "qiniuAccessKey", qiniuAccessKey);
    }

    public String getQiniuSecretKey() {
        return setting.getStr("qiniuSecretKey", "setting.qiniu", "");
    }

    public void setQiniuSecretKey(String qiniuSecretKey) {
        setting.put("setting.qiniu", "qiniuSecretKey", qiniuSecretKey);
    }

    public String getYunpianApiKey() {
        return setting.getStr("apiKey", "setting.yunpian", "");
    }

    public void setYunpianApiKey(String yunpianApiKey) {
        setting.put("setting.yunpian", "apiKey", yunpianApiKey);
    }

    public boolean isHttpUseProxy() {
        return setting.getBool("httpUseProxy", "setting.http", false);
    }

    public void setHttpUseProxy(boolean httpUseProxy) {
        setting.put("setting.http", "httpUseProxy", String.valueOf(httpUseProxy));
    }

    public String getHttpProxyHost() {
        return setting.getStr("httpProxyHost", "setting.http", "");
    }

    public void setHttpProxyHost(String httpProxyHost) {
        setting.put("setting.http", "httpProxyHost", httpProxyHost);
    }

    public String getHttpProxyPort() {
        return setting.getStr("httpProxyPort", "setting.http", "");
    }

    public void setHttpProxyPort(String httpProxyPort) {
        setting.put("setting.http", "httpProxyPort", httpProxyPort);
    }

    public String getHttpProxyUserName() {
        return setting.getStr("httpProxyUserName", "setting.http", "");
    }

    public void setHttpProxyUserName(String httpProxyUserName) {
        setting.put("setting.http", "httpProxyUserName", httpProxyUserName);
    }

    public String getHttpProxyPassword() {
        return setting.getStr("httpProxyPassword", "setting.http", "");
    }

    public void setHttpProxyPassword(String httpProxyPassword) {
        setting.put("setting.http", "httpProxyPassword", httpProxyPassword);
    }

    public Integer getWxAccountId() {
        return setting.getInt("wxAccountId", "setting.wechat");
    }

    public void setWxAccountId(Integer wxAccountId) {
        setting.put("setting.wechat", "wxAccountId", String.valueOf(wxAccountId));
    }
}
