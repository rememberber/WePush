package com.fangxuele.tool.push.domain;

/**
 * 企业微信小程序通知消息中的一个有序键值项。
 */
public class WxCpMiniProgramContentItem {
    private String key;

    private String value;

    public WxCpMiniProgramContentItem() {
    }

    public WxCpMiniProgramContentItem(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
