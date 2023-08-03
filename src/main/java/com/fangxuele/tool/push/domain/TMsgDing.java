package com.fangxuele.tool.push.domain;

import java.io.Serializable;

public class TMsgDing implements Serializable {

    private String radioType;

    private String dingMsgType;

    private String agentId;

    private String webHook;

    private String content;

    private static final long serialVersionUID = 1L;

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
}