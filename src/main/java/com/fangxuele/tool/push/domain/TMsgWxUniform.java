package com.fangxuele.tool.push.domain;

import java.io.Serializable;

public class TMsgWxUniform implements Serializable {
    private Integer id;

    private Integer msgType;

    private String msgName;

    private String mpTemplateId;

    private String maTemplateId;

    private String mpUrl;

    private String maAppid;

    private String maPagePath;

    private String page;

    private String emphasisKeyword;

    private String kefuMsgType;

    private String content;

    private String title;

    private String imgUrl;

    private String describe;

    private String kefuUrl;

    private String createTime;

    private String modifiedTime;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMsgType() {
        return msgType;
    }

    public void setMsgType(Integer msgType) {
        this.msgType = msgType;
    }

    public String getMsgName() {
        return msgName;
    }

    public void setMsgName(String msgName) {
        this.msgName = msgName == null ? null : msgName.trim();
    }

    public String getMpTemplateId() {
        return mpTemplateId;
    }

    public void setMpTemplateId(String mpTemplateId) {
        this.mpTemplateId = mpTemplateId == null ? null : mpTemplateId.trim();
    }

    public String getMaTemplateId() {
        return maTemplateId;
    }

    public void setMaTemplateId(String maTemplateId) {
        this.maTemplateId = maTemplateId == null ? null : maTemplateId.trim();
    }

    public String getMpUrl() {
        return mpUrl;
    }

    public void setMpUrl(String mpUrl) {
        this.mpUrl = mpUrl == null ? null : mpUrl.trim();
    }

    public String getMaAppid() {
        return maAppid;
    }

    public void setMaAppid(String maAppid) {
        this.maAppid = maAppid == null ? null : maAppid.trim();
    }

    public String getMaPagePath() {
        return maPagePath;
    }

    public void setMaPagePath(String maPagePath) {
        this.maPagePath = maPagePath == null ? null : maPagePath.trim();
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page == null ? null : page.trim();
    }

    public String getEmphasisKeyword() {
        return emphasisKeyword;
    }

    public void setEmphasisKeyword(String emphasisKeyword) {
        this.emphasisKeyword = emphasisKeyword == null ? null : emphasisKeyword.trim();
    }

    public String getKefuMsgType() {
        return kefuMsgType;
    }

    public void setKefuMsgType(String kefuMsgType) {
        this.kefuMsgType = kefuMsgType == null ? null : kefuMsgType.trim();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? null : content.trim();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl == null ? null : imgUrl.trim();
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe == null ? null : describe.trim();
    }

    public String getKefuUrl() {
        return kefuUrl;
    }

    public void setKefuUrl(String kefuUrl) {
        this.kefuUrl = kefuUrl == null ? null : kefuUrl.trim();
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime == null ? null : createTime.trim();
    }

    public String getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(String modifiedTime) {
        this.modifiedTime = modifiedTime == null ? null : modifiedTime.trim();
    }
}