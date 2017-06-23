package com.fangxuele.tool.wechat.push.util;

import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.LogFactory;
import com.xiaoleilu.hutool.setting.dialect.Props;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 配置管理
 * Created by zhouy on 2017/6/14.
 */
public class Config {

    private static final Log logger = LogFactory.get();

    private File file;

    private Props props;

    private static Config ourInstance = new Config();

    private String msgName;

    private String previewUser;

    private int memberCount;

    private String memberSql;

    private String memberFilePath;

    private int pushSuccess;

    private int pushFail;

    private long pushLastTime;

    private long pushLeftTime;

    private int totalRecord;

    private int totalPage;

    private int totalThread;

    private int recordPerPage;

    private int pagePerThread;

    private boolean dryRun;

    private boolean radioStartAt;

    private String textStartAt;

    private boolean radioStopAt;

    private String textStopAt;

    private boolean radioPerDay;

    private String textPerDay;

    private boolean radioPerWeek;

    private String textPerWeekWeek;

    private String textPerWeekTime;

    private String wechatAppId;

    private String wechatAppSecret;

    private String wechatToken;

    private String wechatAesKey;

    private String aliServerUrl;

    private String aliAppKey;

    private String aliAppSecret;

    private String aliSign;

    private String mysqlUrl;

    private String mysqlDatabase;

    private String mysqlUser;

    private String mysqlPassword;

    private String theme;

    private String font;

    private int fontSize;

    public static Config getInstance() {
        return ourInstance;
    }

    private Config() {
        file = new File("config/config.properties");
        File configDir = new File("config/");
        if (!file.exists()) {
            try {
                configDir.mkdirs();
                file.createNewFile();
                originInit();
            } catch (IOException e) {
                logger.error(e);
            }
        }
        props = new Props(file);
    }

    public void setProps(String key, String value) {
        props.setProperty(key, value);
    }

    public String getProps(String key) {
        return props.getProperty(key);
    }

    /**
     * 存盘
     */
    public void save() {
        try {
            props.store(new FileOutputStream(file), null);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    /**
     * 初始化原始数据
     */
    private void originInit() {
        props = new Props(file);

        props.setProperty("msg.msgName", "");
        props.setProperty("msg.previewUser", "");
        props.setProperty("member.count", "0");
        props.setProperty("member.sql", "SELECT openid FROM");
        props.setProperty("member.filePath", "");
        props.setProperty("push.success", "0");
        props.setProperty("push.fail", "0");
        props.setProperty("push.lastTime", "0");
        props.setProperty("push.leftTime", "0");
        props.setProperty("push.totalRecord", "0");
        props.setProperty("push.totalPage", "0");
        props.setProperty("push.totalThread", "0");
        props.setProperty("push.recordPerPage", "500");
        props.setProperty("push.pagePerThread", "3");
        props.setProperty("push.dryRun", "true");
        props.setProperty("schedule.radioStartAt", "false");
        props.setProperty("schedule.textStartAt", "");
        props.setProperty("schedule.radioStopAt", "false");
        props.setProperty("schedule.textStopAt", "");
        props.setProperty("schedule.radioPerDay", "false");
        props.setProperty("schedule.textPerDay", "");
        props.setProperty("schedule.radioPerWeek", "false");
        props.setProperty("schedule.textPerWeek.week", "一");
        props.setProperty("schedule.textPerWeek.time", "");
        props.setProperty("setting.wechat.appId", "");
        props.setProperty("setting.wechat.AppSecret", "");
        props.setProperty("setting.wechat.token", "");
        props.setProperty("setting.wechat.aesKey", "");
        props.setProperty("setting.ali.serverUrl", "");
        props.setProperty("setting.ali.appKey", "");
        props.setProperty("setting.ali.appSecret", "");
        props.setProperty("setting.ali.sign", "");
        props.setProperty("setting.mysql.url", "");
        props.setProperty("setting.mysql.database", "");
        props.setProperty("setting.mysql.user", "");
        props.setProperty("setting.mysql.password", "");
        props.setProperty("setting.appearance.theme", "Darcula(推荐)");
        props.setProperty("setting.appearance.font", "Microsoft YaHei UI");
        props.setProperty("setting.appearance.fontSize", "18");

        save();
    }

    public String getMsgName() {
        return props.getProperty("msg.msgName");
    }

    public void setMsgName(String msgName) {
        props.setProperty("msg.msgName", msgName);
    }

    public String getPreviewUser() {
        return props.getProperty("msg.previewUser");
    }

    public void setPreviewUser(String previewUser) {
        props.setProperty("msg.previewUser", previewUser);
    }

    public int getMemberCount() {
        return props.getInt("member.count");
    }

    public void setMemberCount(int memberCount) {
        props.setProperty("member.count", memberCount);
    }

    public String getMemberSql() {
        return props.getProperty("member.sql");
    }

    public void setMemberSql(String memberSql) {
        props.setProperty("member.sql", memberSql);
    }

    public String getMemberFilePath() {
        return props.getProperty("member.filePath");
    }

    public void setMemberFilePath(String memberFilePath) {
        props.setProperty("member.filePath", memberFilePath);
    }

    public int getPushSuccess() {
        return props.getInt("push.success");
    }

    public void setPushSuccess(int pushSuccess) {
        props.setProperty("push.success", pushSuccess);
    }

    public int getPushFail() {
        return props.getInt("push.fail");
    }

    public void setPushFail(int pushFail) {
        props.setProperty("push.fail", pushFail);
    }

    public long getPushLastTime() {
        return props.getLong("push.lastTime");
    }

    public void setPushLastTime(long pushLastTime) {
        props.setProperty("push.lastTime", pushLastTime);
    }

    public long getPushLeftTime() {
        return props.getLong("push.leftTime");
    }

    public void setPushLeftTime(long pushLeftTime) {
        props.setProperty("push.leftTime", pushLeftTime);
    }

    public int getTotalRecord() {
        return props.getInt("push.totalRecord");
    }

    public void setTotalRecord(int totalRecord) {
        props.setProperty("push.totalRecord", totalRecord);
    }

    public int getTotalPage() {
        return props.getInt("push.totalPage");
    }

    public void setTotalPage(int totalPage) {
        props.setProperty("push.totalPage", totalPage);
    }

    public int getTotalThread() {
        return props.getInt("push.totalThread");
    }

    public void setTotalThread(int totalThread) {
        props.setProperty("push.totalThread", totalThread);
    }

    public int getRecordPerPage() {
        return props.getInt("push.recordPerPage");
    }

    public void setRecordPerPage(int recordPerPage) {
        props.setProperty("push.recordPerPage", recordPerPage);
    }

    public int getPagePerThread() {
        return props.getInt("push.pagePerThread");
    }

    public void setPagePerThread(int pagePerThread) {
        props.setProperty("push.pagePerThread", pagePerThread);
    }

    public boolean isDryRun() {
        return props.getBool("push.dryRun");
    }

    public void setDryRun(boolean dryRun) {
        props.setProperty("push.dryRun", dryRun);
    }

    public boolean isRadioStartAt() {
        return props.getBool("schedule.radioStartAt");
    }

    public void setRadioStartAt(boolean radioStartAt) {
        props.setProperty("schedule.radioStartAt", radioStartAt);
    }

    public String getTextStartAt() {
        return props.getProperty("schedule.textStartAt");
    }

    public void setTextStartAt(String textStartAt) {
        props.setProperty("schedule.textStartAt", textStartAt);
    }

    public boolean isRadioStopAt() {
        return props.getBool("schedule.radioStopAt");
    }

    public void setRadioStopAt(boolean radioStopAt) {
        props.setProperty("schedule.radioStopAt", radioStopAt);
    }

    public String getTextStopAt() {
        return props.getProperty("schedule.textStopAt");
    }

    public void setTextStopAt(String textStopAt) {
        props.setProperty("schedule.textStopAt", textStopAt);
    }

    public boolean isRadioPerDay() {
        return props.getBool("schedule.radioPerDay");
    }

    public void setRadioPerDay(boolean radioPerDay) {
        props.setProperty("schedule.radioPerDay", radioPerDay);
    }

    public String getTextPerDay() {
        return props.getProperty("schedule.textPerDay");
    }

    public void setTextPerDay(String textPerDay) {
        props.setProperty("schedule.textPerDay", textPerDay);
    }

    public boolean isRadioPerWeek() {
        return props.getBool("schedule.radioPerWeek");
    }

    public void setRadioPerWeek(boolean radioPerWeek) {
        props.setProperty("schedule.radioPerWeek", radioPerWeek);
    }

    public String getTextPerWeekWeek() {
        return props.getProperty("schedule.textPerWeek.week");
    }

    public void setTextPerWeekWeek(String textPerWeekWeek) {
        props.setProperty("schedule.textPerWeek.week", textPerWeekWeek);
    }

    public String getTextPerWeekTime() {
        return props.getProperty("schedule.textPerWeek.time");
    }

    public void setTextPerWeekTime(String textPerWeekTime) {
        props.setProperty("schedule.textPerWeek.time", textPerWeekTime);
    }

    public String getWechatAppId() {
        return props.getProperty("setting.wechat.appId");
    }

    public void setWechatAppId(String wechatAppId) {
        props.setProperty("setting.wechat.appId", wechatAppId);
    }

    public String getWechatAppSecret() {
        return props.getProperty("setting.wechat.AppSecret");
    }

    public void setWechatAppSecret(String wechatAppSecret) {
        props.setProperty("setting.wechat.AppSecret", wechatAppSecret);
    }

    public String getWechatToken() {
        return props.getProperty("setting.wechat.token");
    }

    public void setWechatToken(String wechatToken) {
        props.setProperty("setting.wechat.token", wechatToken);
    }

    public String getWechatAesKey() {
        return props.getProperty("setting.wechat.aesKey");
    }

    public void setWechatAesKey(String wechatAesKey) {
        props.setProperty("setting.wechat.aesKey", wechatAesKey);
    }

    public String getAliServerUrl() {
        return props.getProperty("setting.ali.serverUrl");
    }

    public void setAliServerUrl(String aliServerUrl) {
        props.setProperty("setting.ali.serverUrl", aliServerUrl);
    }

    public String getAliAppKey() {
        return props.getProperty("setting.ali.appKey");
    }

    public void setAliAppKey(String aliAppKey) {
        props.setProperty("setting.ali.appKey", aliAppKey);
    }

    public String getAliAppSecret() {
        return props.getProperty("setting.ali.appSecret");
    }

    public void setAliAppSecret(String aliAppSecret) {
        props.setProperty("setting.ali.appSecret", aliAppSecret);
    }

    public String getAliSign() {
        return props.getProperty("setting.ali.sign");
    }

    public void setAliSign(String aliSign) {
        props.setProperty("setting.ali.sign", aliSign);
    }

    public String getMysqlUrl() {
        return props.getProperty("setting.mysql.url");
    }

    public void setMysqlUrl(String mysqlUrl) {
        props.setProperty("setting.mysql.url", mysqlUrl);
    }

    public String getMysqlDatabase() {
        return props.getProperty("setting.mysql.database");
    }

    public void setMysqlDatabase(String mysqlDatabase) {
        props.setProperty("setting.mysql.database", mysqlDatabase);
    }

    public String getMysqlUser() {
        return props.getProperty("setting.mysql.user");
    }

    public void setMysqlUser(String mysqlUser) {
        props.setProperty("setting.mysql.user", mysqlUser);
    }

    public String getMysqlPassword() {
        return props.getProperty("setting.mysql.password");
    }

    public void setMysqlPassword(String mysqlPassword) {
        props.setProperty("setting.mysql.password", mysqlPassword);
    }

    public String getTheme() {
        return props.getProperty("setting.appearance.theme");
    }

    public void setTheme(String theme) {
        props.setProperty("setting.appearance.theme", theme);
    }

    public String getFont() {
        return props.getProperty("setting.appearance.font");
    }

    public void setFont(String font) {
        props.setProperty("setting.appearance.font", font);
    }

    public int getFontSize() {
        return props.getInt("setting.appearance.fontSize");
    }

    public void setFontSize(int fontSize) {
        props.setProperty("setting.appearance.fontSize", fontSize);
    }
}
