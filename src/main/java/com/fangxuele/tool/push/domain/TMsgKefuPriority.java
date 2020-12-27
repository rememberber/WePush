package com.fangxuele.tool.push.domain;

import java.io.Serializable;

public class TMsgKefuPriority implements Serializable {
    private Integer id;

    private Integer msgType;

    private String msgName;

    private String templateId;

    private String url;

    private String maAppid;

    private String maPagePath;

    private String kefuMsgType;

    private String content;

    private String title;

    private String imgUrl;

    private String describe;

    private String kefuUrl;

    private String createTime;

    private String modifiedTime;

    private String previewUser;

    private Integer wxAccountId;

    private String appId;

    private String pagePath;

    private String thumbMediaId;

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

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId == null ? null : templateId.trim();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url == null ? null : url.trim();
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

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId == null ? null : appId.trim();
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath == null ? null : pagePath.trim();
    }

    public String getThumbMediaId() {
        return thumbMediaId;
    }

    public void setThumbMediaId(String thumbMediaId) {
        this.thumbMediaId = thumbMediaId == null ? null : thumbMediaId.trim();
    }
}