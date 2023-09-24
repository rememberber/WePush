package com.fangxuele.tool.push.domain;

import java.io.Serializable;

public class TTask implements Serializable {
    private Integer id;

    private String title;

    private Integer msgType;

    private Integer accountId;

    private Integer messageId;

    private Integer peopleId;

    private Integer taskMode;

    private Integer taskPeriod;

    private Integer periodType;

    private String periodTime;

    private String cron;

    private Integer threadCnt;

    private Integer maxThreadCnt;

    private Integer reimportPeople;

    private Integer resultAlert;

    private String alertEmails;

    private Integer saveResult;

    private Integer intervalPush;

    private Integer intervalTime;

    private String remark;

    private String createTime;

    private String modifiedTime;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

    public Integer getMsgType() {
        return msgType;
    }

    public void setMsgType(Integer msgType) {
        this.msgType = msgType;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    public Integer getPeopleId() {
        return peopleId;
    }

    public void setPeopleId(Integer peopleId) {
        this.peopleId = peopleId;
    }

    public Integer getTaskMode() {
        return taskMode;
    }

    public void setTaskMode(Integer taskMode) {
        this.taskMode = taskMode;
    }

    public Integer getTaskPeriod() {
        return taskPeriod;
    }

    public void setTaskPeriod(Integer taskPeriod) {
        this.taskPeriod = taskPeriod;
    }

    public Integer getPeriodType() {
        return periodType;
    }

    public void setPeriodType(Integer periodType) {
        this.periodType = periodType;
    }

    public String getPeriodTime() {
        return periodTime;
    }

    public void setPeriodTime(String periodTime) {
        this.periodTime = periodTime == null ? null : periodTime.trim();
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron == null ? null : cron.trim();
    }

    public Integer getThreadCnt() {
        return threadCnt;
    }

    public void setThreadCnt(Integer threadCnt) {
        this.threadCnt = threadCnt;
    }

    public Integer getMaxThreadCnt() {
        return maxThreadCnt;
    }

    public void setMaxThreadCnt(Integer maxThreadCnt) {
        this.maxThreadCnt = maxThreadCnt;
    }

    public Integer getReimportPeople() {
        return reimportPeople;
    }

    public void setReimportPeople(Integer reimportPeople) {
        this.reimportPeople = reimportPeople;
    }

    public Integer getResultAlert() {
        return resultAlert;
    }

    public void setResultAlert(Integer resultAlert) {
        this.resultAlert = resultAlert;
    }

    public String getAlertEmails() {
        return alertEmails;
    }

    public void setAlertEmails(String alertEmails) {
        this.alertEmails = alertEmails == null ? null : alertEmails.trim();
    }

    public Integer getSaveResult() {
        return saveResult;
    }

    public void setSaveResult(Integer saveResult) {
        this.saveResult = saveResult;
    }

    public Integer getIntervalPush() {
        return intervalPush;
    }

    public void setIntervalPush(Integer intervalPush) {
        this.intervalPush = intervalPush;
    }

    public Integer getIntervalTime() {
        return intervalTime;
    }

    public void setIntervalTime(Integer intervalTime) {
        this.intervalTime = intervalTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark == null ? null : remark.trim();
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime == null ? null : createTime.trim();
    }

    public String getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(String modifiedTime) {
        this.modifiedTime = modifiedTime == null ? null : modifiedTime.trim();
    }
}