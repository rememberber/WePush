package com.fangxuele.tool.push.domain;

import java.io.Serializable;

public class TPushHistory implements Serializable {
    private Integer id;

    private Integer msg_id;

    private Integer msg_type;

    private String msg_primary;

    private String msg_data;

    private String create_time;

    private String modified_time;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMsg_id() {
        return msg_id;
    }

    public void setMsg_id(Integer msg_id) {
        this.msg_id = msg_id;
    }

    public Integer getMsg_type() {
        return msg_type;
    }

    public void setMsg_type(Integer msg_type) {
        this.msg_type = msg_type;
    }

    public String getMsg_primary() {
        return msg_primary;
    }

    public void setMsg_primary(String msg_primary) {
        this.msg_primary = msg_primary == null ? null : msg_primary.trim();
    }

    public String getMsg_data() {
        return msg_data;
    }

    public void setMsg_data(String msg_data) {
        this.msg_data = msg_data == null ? null : msg_data.trim();
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