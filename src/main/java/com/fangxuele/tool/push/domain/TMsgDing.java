package com.fangxuele.tool.push.domain;

import java.io.Serializable;

public class TMsgDing implements Serializable {
    private Integer id;

    private Integer msgType;

    private String msgName;

    private String radioType;

    private String dingMsgType;

    private String agentId;

    private String webHook;

    private String content;

    private String createTime;

    private String modifiedTime;

    private String previewUser;

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

    public String getRadioType() {
        return radioType;
    }

    public void setRadioType(String radioType) {
        this.radioType = radioType == null ? null : radioType.trim();
    }

    public String getDingMsgType() {
        return dingMsgType;
    }

    public void setDingMsgType(String dingMsgType) {
        this.dingMsgType = dingMsgType == null ? null : dingMsgType.trim();
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId == null ? null : agentId.trim();
    }

    public String getWebHook() {
        return webHook;
    }

    public void setWebHook(String webHook) {
        this.webHook = webHook == null ? null : webHook.trim();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? null : content.trim();
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
}