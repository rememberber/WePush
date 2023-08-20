package com.fangxuele.tool.push.domain;

import com.fangxuele.tool.push.bean.TemplateData;

import java.io.Serializable;
import java.util.List;

public class TMsgWxUniform implements Serializable {

    private String mpTemplateId;

    private String maTemplateId;

    private String mpUrl;

    private String maAppid;

    private String maPagePath;

    private String page;

    private String emphasisKeyword;

    List<TemplateData> templateDataListMp;

    List<TemplateData> templateDataListMa;

    private static final long serialVersionUID = 1L;


    public String getMpTemplateId() {
        return mpTemplateId;
    }

    public void setMpTemplateId(String mpTemplateId) {
        this.mpTemplateId = mpTemplateId == null ? null : mpTemplateId.trim();
    }

    public String getMaTemplateId() {
        return maTemplateId;
    }

    public void setMaTemplateId(String maTemplateId) {
        this.maTemplateId = maTemplateId == null ? null : maTemplateId.trim();
    }

    public String getMpUrl() {
        return mpUrl;
    }

    public void setMpUrl(String mpUrl) {
        this.mpUrl = mpUrl == null ? null : mpUrl.trim();
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

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page == null ? null : page.trim();
    }

    public String getEmphasisKeyword() {
        return emphasisKeyword;
    }

    public void setEmphasisKeyword(String emphasisKeyword) {
        this.emphasisKeyword = emphasisKeyword == null ? null : emphasisKeyword.trim();
    }

    public List<TemplateData> getTemplateDataListMp() {
        return templateDataListMp;
    }

    public void setTemplateDataListMp(List<TemplateData> templateDataListMp) {
        this.templateDataListMp = templateDataListMp;
    }

    public List<TemplateData> getTemplateDataListMa() {
        return templateDataListMa;
    }

    public void setTemplateDataListMa(List<TemplateData> templateDataListMa) {
        this.templateDataListMa = templateDataListMa;
    }
}