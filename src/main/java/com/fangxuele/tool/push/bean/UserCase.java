package com.fangxuele.tool.push.bean;

import java.io.Serializable;

public class UserCase implements Serializable {

    private static final long serialVersionUID = 2829237163275443844L;

    private String qrCodeUrl;

    private String title;

    private String desc;

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
