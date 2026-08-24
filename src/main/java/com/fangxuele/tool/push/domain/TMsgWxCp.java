package com.fangxuele.tool.push.domain;

import java.util.List;

public class TMsgWxCp {

    private String cpMsgType;

    private String content;

    private String title;

    private String imgUrl;

    private String describe;

    private String url;

    private String btnTxt;

    private String miniProgramAppId;

    private String miniProgramPage;

    private Boolean miniProgramEmphasisFirstItem;

    private List<WxCpMiniProgramContentItem> miniProgramContentItems;

    private Boolean enableIdTrans;

    private Boolean enableDuplicateCheck;

    private Integer duplicateCheckInterval;

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

    public String getMiniProgramAppId() {
        return miniProgramAppId;
    }

    public void setMiniProgramAppId(String miniProgramAppId) {
        this.miniProgramAppId = miniProgramAppId == null ? null : miniProgramAppId.trim();
    }

    public String getMiniProgramPage() {
        return miniProgramPage;
    }

    public void setMiniProgramPage(String miniProgramPage) {
        this.miniProgramPage = miniProgramPage == null ? null : miniProgramPage.trim();
    }

    public Boolean getMiniProgramEmphasisFirstItem() {
        return miniProgramEmphasisFirstItem;
    }

    public void setMiniProgramEmphasisFirstItem(Boolean miniProgramEmphasisFirstItem) {
        this.miniProgramEmphasisFirstItem = miniProgramEmphasisFirstItem;
    }

    public List<WxCpMiniProgramContentItem> getMiniProgramContentItems() {
        return miniProgramContentItems;
    }

    public void setMiniProgramContentItems(List<WxCpMiniProgramContentItem> miniProgramContentItems) {
        this.miniProgramContentItems = miniProgramContentItems;
    }

    public Boolean getEnableIdTrans() {
        return enableIdTrans;
    }

    public void setEnableIdTrans(Boolean enableIdTrans) {
        this.enableIdTrans = enableIdTrans;
    }

    public Boolean getEnableDuplicateCheck() {
        return enableDuplicateCheck;
    }

    public void setEnableDuplicateCheck(Boolean enableDuplicateCheck) {
        this.enableDuplicateCheck = enableDuplicateCheck;
    }

    public Integer getDuplicateCheckInterval() {
        return duplicateCheckInterval;
    }

    public void setDuplicateCheckInterval(Integer duplicateCheckInterval) {
        this.duplicateCheckInterval = duplicateCheckInterval;
    }

}
