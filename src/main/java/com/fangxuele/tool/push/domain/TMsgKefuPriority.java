package com.fangxuele.tool.push.domain;

import java.io.Serializable;

public class TMsgKefuPriority implements Serializable {
    private Integer id;

    private Integer msg_type;

    private String msg_name;

    private String template_id;

    private String url;

    private String ma_appid;

    private String ma_page_path;

    private String kefu_msg_type;

    private String content;

    private String title;

    private String img_url;

    private String describe;

    private String kefu_url;

    private String create_time;

    private String modified_time;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMsg_type() {
        return msg_type;
    }

    public void setMsg_type(Integer msg_type) {
        this.msg_type = msg_type;
    }

    public String getMsg_name() {
        return msg_name;
    }

    public void setMsg_name(String msg_name) {
        this.msg_name = msg_name == null ? null : msg_name.trim();
    }

    public String getTemplate_id() {
        return template_id;
    }

    public void setTemplate_id(String template_id) {
        this.template_id = template_id == null ? null : template_id.trim();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url == null ? null : url.trim();
    }

    public String getMa_appid() {
        return ma_appid;
    }

    public void setMa_appid(String ma_appid) {
        this.ma_appid = ma_appid == null ? null : ma_appid.trim();
    }

    public String getMa_page_path() {
        return ma_page_path;
    }

    public void setMa_page_path(String ma_page_path) {
        this.ma_page_path = ma_page_path == null ? null : ma_page_path.trim();
    }

    public String getKefu_msg_type() {
        return kefu_msg_type;
    }

    public void setKefu_msg_type(String kefu_msg_type) {
        this.kefu_msg_type = kefu_msg_type == null ? null : kefu_msg_type.trim();
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

    public String getImg_url() {
        return img_url;
    }

    public void setImg_url(String img_url) {
        this.img_url = img_url == null ? null : img_url.trim();
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe == null ? null : describe.trim();
    }

    public String getKefu_url() {
        return kefu_url;
    }

    public void setKefu_url(String kefu_url) {
        this.kefu_url = kefu_url == null ? null : kefu_url.trim();
    }

    public String getCreate_time() {
        return create_time;
    }

    public void setCreate_time(String create_time) {
        this.create_time = create_time == null ? null : create_time.trim();
    }

    public String getModified_time() {
        return modified_time;
    }

    public void setModified_time(String modified_time) {
        this.modified_time = modified_time == null ? null : modified_time.trim();
    }
}