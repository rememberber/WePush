package com.fangxuele.tool.push.domain;

import com.fangxuele.tool.push.bean.TemplateData;

import java.io.Serializable;
import java.util.List;

public class TMsgMaSubscribe implements Serializable {
    private String templateId;

    private String page;

    List<TemplateData> templateDataList;

    private static final long serialVersionUID = 1L;

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId == null ? null : templateId.trim();
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page == null ? null : page.trim();
    }

    public List<TemplateData> getTemplateDataList() {
        return templateDataList;
    }

    public void setTemplateDataList(List<TemplateData> templateDataList) {
        this.templateDataList = templateDataList;
    }
}