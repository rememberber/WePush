package com.fangxuele.tool.push.domain;

import java.io.Serializable;

public class TMsgSms implements Serializable {
    private Integer id;

    private Integer msg_type;

    private String msg_name;

    private String template_id;

    private String content;

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? null : content.trim();
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