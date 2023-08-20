package com.fangxuele.tool.push.domain;

import java.io.Serializable;

public class TMsgWxCp implements Serializable {

    private String cpMsgType;

    private String content;

    private String title;

    private String imgUrl;

    private String describe;

    private String url;

    private String btnTxt;

    private static final long serialVersionUID = 1L;

    public String getCpMsgType() {
        return cpMsgType;
    }

    public void setCpMsgType(String cpMsgType) {
        this.cpMsgType = cpMsgType == null ? null : cpMsgType.trim();
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url == null ? null : url.trim();
    }

    public String getBtnTxt() {
        return btnTxt;
    }

    public void setBtnTxt(String btnTxt) {
        this.btnTxt = btnTxt == null ? null : btnTxt.trim();
    }

}