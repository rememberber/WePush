package com.fangxuele.tool.push.domain;

import com.fangxuele.tool.push.bean.TemplateData;

import java.util.List;

/**
 * 极光推送消息内容
 */
public class TMsgJiguangPush {

    /**
     * 目标类型：alias-别名，registration_id-RegistrationId
     */
    private String audienceType;

    private String title;

    private String content;

    /**
     * iOS是否生产环境
     */
    private boolean apnsProduction;

    private List<TemplateData> extrasList;

    public String getAudienceType() {
        return audienceType;
    }

    public void setAudienceType(String audienceType) {
        this.audienceType = audienceType == null ? null : audienceType.trim();
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

    public boolean isApnsProduction() {
        return apnsProduction;
    }

    public void setApnsProduction(boolean apnsProduction) {
        this.apnsProduction = apnsProduction;
    }

    public List<TemplateData> getExtrasList() {
        return extrasList;
    }

    public void setExtrasList(List<TemplateData> extrasList) {
        this.extrasList = extrasList;
    }
}
