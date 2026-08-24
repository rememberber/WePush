package com.fangxuele.tool.push.domain;

/**
 * 飞书自定义机器人消息配置。
 */
public class TMsgFeishu {
    private String feishuMsgType;

    private String title;

    private String content;

    private String mentionType;

    public String getFeishuMsgType() {
        return feishuMsgType;
    }

    public void setFeishuMsgType(String feishuMsgType) {
        this.feishuMsgType = feishuMsgType == null ? null : feishuMsgType.trim();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? null : content.trim();
    }

    public String getMentionType() {
        return mentionType;
    }

    public void setMentionType(String mentionType) {
        this.mentionType = mentionType == null ? null : mentionType.trim();
    }
}
