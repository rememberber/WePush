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

    private String createTime;

    private String modifiedTime;

    private String previewUser;

    private Integer wxAccountId;

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

    public String getPreviewUser() {
        return previewUser;
    }

    public void setPreviewUser(String previewUser) {
        this.previewUser = previewUser == null ? null : previewUser.trim();
    }

    public Integer getWxAccountId() {
        return wxAccountId;
    }

    public void setWxAccountId(Integer wxAccountId) {
        this.wxAccountId = wxAccountId;
    }
}