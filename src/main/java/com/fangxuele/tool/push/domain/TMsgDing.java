package com.fangxuele.tool.push.domain;

import java.io.Serializable;

public class TMsgDing implements Serializable {

    private String radioType;

    private String dingMsgType;

    private String webHook;

    private String content;

    private  String msgTitle;

    private String picUrl;

    private String url;

    private String btnTxt;

    private String btnUrl;

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

    public String getMsgTitle() {
        return msgTitle;
    }

    public void setMsgTitle(String msgTitle) {
        this.msgTitle = msgTitle;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBtnTxt() {
        return btnTxt;
    }

    public void setBtnTxt(String btnTxt) {
        this.btnTxt = btnTxt;
    }

    public String getBtnUrl() {
        return btnUrl;
    }

    public void setBtnUrl(String btnUrl) {
        this.btnUrl = btnUrl;
    }
}